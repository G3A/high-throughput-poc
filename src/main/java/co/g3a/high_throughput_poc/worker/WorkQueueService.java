package co.g3a.high_throughput_poc.worker;

import co.g3a.high_throughput_poc.worker.exception.*;
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
    private final Duration defaultMaxProcessingTime = Duration.ofMinutes(1);
    private final Duration taskResultsRetention = Duration.ofSeconds(10);

    // Control de carga
    private final Semaphore requestThrottle;
    private static final int MAX_CONCURRENT_REQUESTS = 2000;

    // Métricas
    private final AtomicLong totalTasksProcessed = new AtomicLong(0);
    private final AtomicLong tasksRejected = new AtomicLong(0);
    private final AtomicLong tasksSuccessful = new AtomicLong(0);
    private final ConcurrentMap<String, AtomicLong> taskCountByType = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Long> processingTimes = new ConcurrentLinkedQueue<>();
    private final int maxProcessingTimeSamples = 300;
    private final Instant startTime = Instant.now();

    public WorkQueueService(List<TaskProcessor<?, ?>> taskProcessors) {
        this.requestThrottle = new Semaphore(MAX_CONCURRENT_REQUESTS);

        taskProcessors.forEach(processor -> {
            processors.put(processor.getTaskType(), processor);
            taskCountByType.put(processor.getTaskType(), new AtomicLong(0));
        });

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
        boolean permitAcquired = false;
        Instant startTime = Instant.now();

        try {
            permitAcquired = requestThrottle.tryAcquire(5, TimeUnit.SECONDS);
            if (!permitAcquired) {
                task.setStatus(WorkTask.TaskStatus.REJECTED);
                task.setProcessedAt(Instant.now());
                tasksRejected.incrementAndGet();
                taskResults.put(task.getId(), task);
                notifyClient(task);
                throw new ServerHighLoadException(requestThrottle.availablePermits());
            }

            TaskProcessor<T, R> processor = (TaskProcessor<T, R>) processors.get(task.getType());
            if (processor == null) {
                throw new ProcessorNotFoundException(task.getType());
            }

            R result = processor.processTask(task.getRequest());
            task.setResult(result);

            Duration processingDuration = Duration.between(startTime, Instant.now());

            totalTasksProcessed.incrementAndGet();
            taskCountByType.get(task.getType()).incrementAndGet();

            synchronized (processingTimes) {
                processingTimes.add(processingDuration.toMillis());
                while (processingTimes.size() > maxProcessingTimeSamples) {
                    processingTimes.poll();
                }
            }

            if (processingDuration.compareTo(task.getMaxProcessingTime()) > 0) {
                task.setStatus(WorkTask.TaskStatus.REJECTED);
                tasksRejected.incrementAndGet();
                throw new ProcessingTimeoutException(task.getId(), processingDuration);
            } else {
                task.setStatus(WorkTask.TaskStatus.PROCESSED);
                tasksSuccessful.incrementAndGet();
            }

            taskResults.put(task.getId(), task);

        } catch (ProcessorNotFoundException | ServerHighLoadException | ProcessingTimeoutException e) {
            // Estas excepciones ya están manejadas antes de lanzarse
            throw e;
        } catch (Exception e) {
            task.setStatus(WorkTask.TaskStatus.REJECTED);
            task.setProcessedAt(Instant.now());
            tasksRejected.incrementAndGet();
            TaskProcessingException processingException = new TaskProcessingException(
                    task.getId(), task.getType(), e.getMessage(), e
            );
            taskResults.put(task.getId(), task);
            throw processingException;
        } finally {
            if (permitAcquired) {
                requestThrottle.release();
            }
            notifyClient(task);
        }
    }

    private void notifyClient(WorkTask<?, ?> task) {
        SseEmitter emitter = emitters.remove(task.getId());
        if (emitter != null) {
            try {
                Map<String, Object> response = new HashMap<>();
                response.put("idTask", task.getId());
                response.put("status", task.getStatus().toString());

                if (task.getStatus() == WorkTask.TaskStatus.PROCESSED) {
                    response.put("result", task.getResult() != null ? task.getResult() : "");
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
        if (requestThrottle.availablePermits() < MAX_CONCURRENT_REQUESTS * 0.03 ) {
            // El sistema solo rechazará nuevas tareas cuando queden menos de 90 permisos disponibles de los 3000 totales.
            throw new ServerHighLoadException(requestThrottle.availablePermits());
        }

        if (!processors.containsKey(type)) {
            throw new ProcessorNotFoundException(type);
        }

        WorkTask<T, R> task = new WorkTask<>(type, request, maxProcessingTime);
        taskResults.put(task.getId(), task);
        processingExecutor.submit(() -> {
            try {
                processTaskImmediately(task);
            } catch (WorkQueueException e) {
                // Las excepciones ya fueron manejadas en processTaskImmediately
                // Solo capturamos aquí para evitar que el executor falle
            }
        });

        return task.getId();
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, Object> processTaskAndWaitResult(String type, T request) {
        Instant taskStartTime = Instant.now();

        if (!processors.containsKey(type)) {
            throw new ProcessorNotFoundException(type);
        }

        try {
            TaskProcessor<T, ?> processor = (TaskProcessor<T, ?>) processors.get(type);
            Object result = processor.processTask(request);

            Duration processingDuration = Duration.between(taskStartTime, Instant.now());
            totalTasksProcessed.incrementAndGet();
            tasksSuccessful.incrementAndGet();
            taskCountByType.get(type).incrementAndGet();

            synchronized (processingTimes) {
                processingTimes.add(processingDuration.toMillis());
                while (processingTimes.size() > maxProcessingTimeSamples) {
                    processingTimes.poll();
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", WorkTask.TaskStatus.PROCESSED.toString());
            response.put("result", result);
            response.put("processingTimeMs", processingDuration.toMillis());
            return response;

        } catch (Exception e) {
            tasksRejected.incrementAndGet();
            throw new TaskProcessingException(UUID.randomUUID(), type, e.getMessage(), e);
        }
    }

    public Map<String, Object> getTaskStatus(UUID taskId) {
        WorkTask<?, ?> task = taskResults.get(taskId);

        if (task == null) {
            throw new TaskNotFoundException(taskId);
        }

        Map<String, Object> status = new HashMap<>();
        status.put("idTask", taskId);
        status.put("taskType", task.getType());
        status.put("createdAt", task.getCreatedAt().toString());
        status.put("status", task.getStatus().toString());

        switch (task.getStatus()) {
            case PENDING:
                status.put("elapsedTimeMs", Duration.between(task.getCreatedAt(), Instant.now()).toMillis());
                break;
            case PROCESSED:
                status.put("result", task.getResult());
                status.put("processingTimeMs", task.getProcessingDuration().toMillis());
                status.put("processedAt", task.getProcessedAt().toString());
                break;
            case REJECTED:
                status.put("processingTimeMs", task.getProcessingDuration().toMillis());
                status.put("processedAt", task.getProcessedAt().toString());
                break;
        }

        return status;
    }

    public SseEmitter createEmitterForTask(UUID taskId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        WorkTask<?, ?> existingTask = taskResults.get(taskId);
        if (existingTask == null) {
            try {
                Map<String, Object> response = new HashMap<>();
                response.put("idTask", taskId);
                response.put("status", "UNKNOWN");
                response.put("message", "Task not found or expired");

                emitter.send(SseEmitter.event().data(response));
                emitter.complete();
                return emitter;
            } catch (IOException e) {
                emitter.completeWithError(e);
                return emitter;
            }
        }

        if (existingTask.getStatus() != WorkTask.TaskStatus.PENDING) {
            try {
                Map<String, Object> response = new HashMap<>();
                response.put("idTask", existingTask.getId());
                response.put("status", existingTask.getStatus().toString());

                if (existingTask.getStatus() == WorkTask.TaskStatus.PROCESSED) {
                    response.put("result", existingTask.getResult() != null ? existingTask.getResult() : "");
                }

                emitter.send(SseEmitter.event().data(response));
                emitter.complete();
                return emitter;
            } catch (IOException e) {
                emitter.completeWithError(e);
                return emitter;
            }
        }

        emitter.onCompletion(() -> emitters.remove(taskId));
        emitter.onTimeout(() -> emitters.remove(taskId));
        emitter.onError(e -> emitters.remove(taskId));

        emitters.put(taskId, emitter);
        return emitter;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalTasksProcessed", totalTasksProcessed.get());
        stats.put("tasksSuccessful", tasksSuccessful.get());
        stats.put("tasksRejected", tasksRejected.get());
        stats.put("activeEmitters", emitters.size());
        stats.put("storedResults", taskResults.size());
        stats.put("uptime", Duration.between(startTime, Instant.now()).getSeconds());

        stats.put("availablePermits", requestThrottle.availablePermits());
        stats.put("queueLength", MAX_CONCURRENT_REQUESTS - requestThrottle.availablePermits());
        stats.put("systemLoad", (double)(MAX_CONCURRENT_REQUESTS - requestThrottle.availablePermits()) / MAX_CONCURRENT_REQUESTS);

        Map<String, Long> tasksByType = new HashMap<>();
        taskCountByType.forEach((type, count) -> tasksByType.put(type, count.get()));
        stats.put("tasksByType", tasksByType);

        List<String> registeredProcessors = new ArrayList<>(processors.keySet());
        stats.put("registeredProcessors", registeredProcessors);

        return stats;
    }
}