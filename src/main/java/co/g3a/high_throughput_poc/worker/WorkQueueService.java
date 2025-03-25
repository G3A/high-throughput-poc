package co.g3a.high_throughput_poc.worker;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WorkQueueService {
    private final ConcurrentMap<String, TaskProcessor<?, ?>> processors = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, WorkTask<?, ?>> taskResults = new ConcurrentHashMap<>();
    private final ExecutorService processingExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Duration defaultMaxProcessingTime = Duration.ofSeconds(7);
    private final Duration taskResultsRetention = Duration.ofMinutes(30); // Mantener resultados por 30 minutos
    
    // Métricas para estadísticas
    private final AtomicLong totalTasksProcessed = new AtomicLong(0);
    private final AtomicLong tasksRejected = new AtomicLong(0);
    private final AtomicLong tasksSuccessful = new AtomicLong(0);
    private final ConcurrentMap<String, AtomicLong> taskCountByType = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Long> processingTimes = new ConcurrentLinkedQueue<>();
    private final int maxProcessingTimeSamples = 100; // Mantener las últimas 100 muestras
    private final Instant startTime = Instant.now();

    public WorkQueueService(List<TaskProcessor<?, ?>> taskProcessors) {
        // Registrar todos los procesadores de tareas disponibles
        taskProcessors.forEach(processor -> {
            processors.put(processor.getTaskType(), processor);
            taskCountByType.put(processor.getTaskType(), new AtomicLong(0));
        });
        
        // Programar limpieza periódica de resultados antiguos
        ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        cleanupExecutor.scheduleAtFixedRate(this::cleanupOldResults, 5, 5, TimeUnit.MINUTES);
    }

    private void cleanupOldResults() {
        Instant now = Instant.now();
        taskResults.entrySet().removeIf(entry -> {
            WorkTask<?, ?> task = entry.getValue();
            return task.getProcessedAt() != null && 
                   Duration.between(task.getProcessedAt(), now).compareTo(taskResultsRetention) > 0;
        });
    }

    @SuppressWarnings("unchecked")
    private <T, R> void processTaskImmediately(WorkTask<T, R> task) {
        Instant startTime = Instant.now();
        
        try {
            // Buscar el procesador adecuado para esta tarea
            TaskProcessor<T, R> processor = (TaskProcessor<T, R>) processors.get(task.getType());
            if (processor == null) {
                throw new IllegalStateException("No processor found for task type: " + task.getType());
            }
            
            // Procesar la tarea inmediatamente
            R result = processor.processTask(task.getRequest());
            task.setResult(result);
            
            // Verificar si tardó demasiado
            Duration processingDuration = Duration.between(startTime, Instant.now());
            
            // Actualizar métricas
            totalTasksProcessed.incrementAndGet();
            taskCountByType.get(task.getType()).incrementAndGet();
            
            // Guardar tiempo de procesamiento
            synchronized (processingTimes) {
                processingTimes.add(processingDuration.toMillis());
                while (processingTimes.size() > maxProcessingTimeSamples) {
                    processingTimes.poll();
                }
            }
            
            if (processingDuration.compareTo(task.getMaxProcessingTime()) > 0) {
                task.setStatus(WorkTask.TaskStatus.REJECTED);
                tasksRejected.incrementAndGet();
            } else {
                task.setStatus(WorkTask.TaskStatus.PROCESSED);
                tasksSuccessful.incrementAndGet();
            }
            
            // Guardar el resultado para futuras consultas
            taskResults.put(task.getId(), task);
        } catch (Exception e) {
            task.setStatus(WorkTask.TaskStatus.REJECTED);
            tasksRejected.incrementAndGet();
            System.err.println("Error processing task: " + e.getMessage());
            e.printStackTrace();
            
            // Guardar el resultado con error
            taskResults.put(task.getId(), task);
        }
        
        // Notificar al cliente si hay un emitter registrado
        notifyClient(task);
    }

    private void notifyClient(WorkTask<?, ?> task) {
        SseEmitter emitter = emitters.remove(task.getId());
        if (emitter != null) {
            try {
                Map<String, Object> response;
                
                if (task.getStatus() == WorkTask.TaskStatus.PROCESSED) {
                    response = Map.of(
                        "idTask", task.getId(),
                        "status", "PROCESSED",
                        "result", task.getResult() != null ? task.getResult() : ""
                    );
                } else {
                    response = Map.of(
                        "idTask", task.getId(),
                        "status", "REJECTED"
                    );
                }
                
                emitter.send(SseEmitter.event().data(response));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }

    public <T, R> UUID enqueueTask(String type, T request) {
        return enqueueTask(type, request, defaultMaxProcessingTime);
    }
    
    public <T, R> UUID enqueueTask(String type, T request, Duration maxProcessingTime) {
        // Verificar que exista un procesador para este tipo de tarea
        if (!processors.containsKey(type)) {
            throw new IllegalArgumentException("No processor registered for task type: " + type);
        }
        
        // Crear la tarea
        WorkTask<T, R> task = new WorkTask<>(type, request, maxProcessingTime);
        
        // Guardar la tarea en estado PENDING
        taskResults.put(task.getId(), task);
        
        // Procesar la tarea inmediatamente en un hilo separado
        processingExecutor.submit(() -> processTaskImmediately(task));
        
        return task.getId();
    }

    /**
     * Procesa una tarea de forma síncrona y devuelve el resultado directamente
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, Object> processTaskAndWaitResult(String type, T request) {
        Instant taskStartTime = Instant.now();
        
        // Verificar que exista un procesador para este tipo de tarea
        if (!processors.containsKey(type)) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "No processor registered for task type: " + type);
            return errorResult;
        }
        
        try {
            // Obtener el procesador adecuado
            TaskProcessor<T, ?> processor = (TaskProcessor<T, ?>) processors.get(type);
            
            // Procesar la tarea y obtener el resultado
            Object result = processor.processTask(request);
            
            // Actualizar métricas
            Duration processingDuration = Duration.between(taskStartTime, Instant.now());
            totalTasksProcessed.incrementAndGet();
            tasksSuccessful.incrementAndGet();
            taskCountByType.get(type).incrementAndGet();
            
            // Guardar tiempo de procesamiento
            synchronized (processingTimes) {
                processingTimes.add(processingDuration.toMillis());
                while (processingTimes.size() > maxProcessingTimeSamples) {
                    processingTimes.poll();
                }
            }
            
            // Preparar la respuesta
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            } else {
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("result", result);
                return responseMap;
            }
        } catch (Exception e) {
            tasksRejected.incrementAndGet();
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Error processing task: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Obtiene el estado actual de una tarea
     */
    public Map<String, Object> getTaskStatus(UUID taskId) {
        Map<String, Object> status = new HashMap<>();
        WorkTask<?, ?> task = taskResults.get(taskId);
        
        if (task == null) {
            status.put("idTask", taskId);
            status.put("status", "UNKNOWN");
            status.put("message", "Task not found or expired");
            return status;
        }
        
        status.put("idTask", taskId);
        status.put("taskType", task.getType());
        status.put("createdAt", task.getCreatedAt().toString());
        
        switch (task.getStatus()) {
            case PENDING:
                status.put("status", "PENDING");
                status.put("elapsedTimeMs", Duration.between(task.getCreatedAt(), Instant.now()).toMillis());
                break;
            case PROCESSED:
                status.put("status", "PROCESSED");
                status.put("result", task.getResult());
                status.put("processingTimeMs", task.getProcessingDuration().toMillis());
                status.put("processedAt", task.getProcessedAt().toString());
                break;
            case REJECTED:
                status.put("status", "REJECTED");
                status.put("processingTimeMs", task.getProcessingDuration().toMillis());
                status.put("processedAt", task.getProcessedAt().toString());
                break;
        }
        
        return status;
    }

    /**
     * Crea un SSE emitter para una tarea específica
     */
    public SseEmitter createEmitterForTask(UUID taskId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Sin timeout práctico
        
        WorkTask<?, ?> existingTask = taskResults.get(taskId);
        if (existingTask != null && existingTask.getStatus() != WorkTask.TaskStatus.PENDING) {
            // Si la tarea ya está completada, enviar el resultado inmediatamente
            try {
                Map<String, Object> response;
                if (existingTask.getStatus() == WorkTask.TaskStatus.PROCESSED) {
                    response = Map.of(
                        "idTask", existingTask.getId(),
                        "status", "PROCESSED",
                        "result", existingTask.getResult() != null ? existingTask.getResult() : ""
                    );
                } else {
                    response = Map.of(
                        "idTask", existingTask.getId(),
                        "status", "REJECTED"
                    );
                }
                emitter.send(SseEmitter.event().data(response));
                emitter.complete();
                return emitter;
            } catch (IOException e) {
                emitter.completeWithError(e);
                return emitter;
            }
        }
        
        // Configurar callbacks
        emitter.onCompletion(() -> emitters.remove(taskId));
        emitter.onTimeout(() -> emitters.remove(taskId));
        emitter.onError(e -> emitters.remove(taskId));
        
        // Guardar el emitter para notificar cuando la tarea esté completa
        emitters.put(taskId, emitter);
        return emitter;
    }
    
    /**
     * Obtiene estadísticas sobre el rendimiento del servicio
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Estadísticas generales
        stats.put("totalTasksProcessed", totalTasksProcessed.get());
        stats.put("tasksSuccessful", tasksSuccessful.get());
        stats.put("tasksRejected", tasksRejected.get());
        stats.put("activeEmitters", emitters.size());
        stats.put("storedResults", taskResults.size());
        stats.put("uptime", Duration.between(startTime, Instant.now()).getSeconds());
        
        // Procesadores registrados
        List<String> registeredProcessors = new ArrayList<>(processors.keySet());
        stats.put("registeredProcessors", registeredProcessors);
        stats.put("processorCount", registeredProcessors.size());
        
        // Conteo por tipo de tarea
        Map<String, Long> tasksByType = new HashMap<>();
        taskCountByType.forEach((type, count) -> tasksByType.put(type, count.get()));
        stats.put("tasksByType", tasksByType);
        
        // Tiempo de procesamiento promedio
        double avgProcessingTime = 0;
        synchronized (processingTimes) {
            if (!processingTimes.isEmpty()) {
                avgProcessingTime = processingTimes.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0);
            }
        }
        stats.put("avgProcessingTimeMs", avgProcessingTime);
        
        return stats;
    }
    
    /**
     * Detiene ordenadamente el servicio de procesamiento de tareas
     */
    public void shutdown() {
        processingExecutor.shutdown();
        try {
            if (!processingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                processingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            processingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}