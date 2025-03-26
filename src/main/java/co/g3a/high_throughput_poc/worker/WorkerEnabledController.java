package co.g3a.high_throughput_poc.worker;

import co.g3a.high_throughput_poc.worker.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class WorkerEnabledController {

    protected final WorkQueueService workQueueService;

    public WorkerEnabledController(WorkQueueService workQueueService) {
        this.workQueueService = workQueueService;
    }

    /**
     * Encola una tarea y configura un SSE emitter para recibir el resultado (asíncrono)
     */
    protected ResponseEntity<?> enqueueTaskAsync(String taskType, Object payload) {
        try {
            // Encolar la tarea para procesamiento inmediato
            UUID taskId = workQueueService.enqueueTask(taskType, payload);

            // Crear un emitter para esta tarea
            SseEmitter emitter = workQueueService.createEmitterForTask(taskId);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
        } catch (ServerHighLoadException e) {
            return buildErrorResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "REJECTED",
                    "Server is currently at high load. Please try again later.",
                    e
            );
        } catch (ProcessorNotFoundException e) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "REJECTED",
                    "No processor found for task type: " + e.getTaskType(),
                    e
            );
        } catch (Exception e) {
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "REJECTED",
                    "Error enqueueing task: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Procesa una tarea y espera su resultado (síncrono)
     */
    protected ResponseEntity<Map<String, Object>> enqueueTask(String taskType, Object payload) {
        try {
            // Procesar la tarea y obtener el resultado de forma síncrona
            Map<String, Object> result = workQueueService.processTaskAndWaitResult(taskType, payload);
            return ResponseEntity.ok(result);
        } catch (ProcessorNotFoundException e) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "REJECTED",
                    "No processor found for task type: " + e.getTaskType(),
                    e
            );
        } catch (TaskProcessingException e) {
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "REJECTED",
                    "Error processing task: " + e.getMessage(),
                    e
            );
        } catch (Exception e) {
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "REJECTED",
                    "Unexpected error processing task: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Encola una tarea y devuelve inmediatamente el ID de la tarea (asíncrono sin SSE)
     */
    protected ResponseEntity<Map<String, Object>> enqueueTaskAndReturnId(String taskType, Object payload) {
        try {
            // Encolar la tarea para procesamiento en segundo plano
            UUID taskId = workQueueService.enqueueTask(taskType, payload);

            // Devolver inmediatamente el ID de la tarea y su estado inicial
            Map<String, Object> response = new HashMap<>();
            response.put("idTask", taskId);
            response.put("status", "ACCEPTED");

            return ResponseEntity.accepted().body(response);
        } catch (ServerHighLoadException e) {
            return buildErrorResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "REJECTED",
                    "Server is currently at high load. Please try again later.",
                    e
            );
        } catch (ProcessorNotFoundException e) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "REJECTED",
                    "No processor found for task type: " + e.getTaskType(),
                    e
            );
        } catch (Exception e) {
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "REJECTED",
                    "Error enqueueing task: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Encola una tarea con tiempo máximo de procesamiento personalizado
     */
    protected ResponseEntity<Map<String, Object>> enqueueTaskWithTimeout(String taskType, Object payload, Duration maxProcessingTime) {
        try {
            // Encolar la tarea con tiempo de procesamiento personalizado
            UUID taskId = workQueueService.enqueueTask(taskType, payload, maxProcessingTime);

            // Devolver inmediatamente el ID de la tarea y su estado inicial
            Map<String, Object> response = new HashMap<>();
            response.put("idTask", taskId);
            response.put("status", "ACCEPTED");
            response.put("maxProcessingTimeMs", maxProcessingTime.toMillis());

            return ResponseEntity.accepted().body(response);
        } catch (ServerHighLoadException e) {
            return buildErrorResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "REJECTED",
                    "Server is currently at high load. Please try again later.",
                    e
            );
        } catch (ProcessorNotFoundException e) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "REJECTED",
                    "No processor found for task type: " + e.getTaskType(),
                    e
            );
        } catch (Exception e) {
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "REJECTED",
                    "Error enqueueing task: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Obtiene el estado actual de una tarea
     */
    protected ResponseEntity<Map<String, Object>> getTaskStatus(UUID taskId) {
        try {
            Map<String, Object> status = workQueueService.getTaskStatus(taskId);
            return ResponseEntity.ok(status);
        } catch (TaskNotFoundException e) {
            return buildErrorResponse(
                    HttpStatus.NOT_FOUND,
                    "UNKNOWN",
                    "Task not found or expired: " + e.getTaskId(),
                    e
            );
        } catch (Exception e) {
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ERROR",
                    "Error retrieving task status: " + e.getMessage(),
                    e
            );
        }
    }


    /**
     * Construye una respuesta de error estándar
     */
    protected ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String taskStatus,
            String message,
            Exception exception) {

        Map<String, Object> response = new HashMap<>();
        response.put("status", taskStatus);
        response.put("message", message);

        if (exception instanceof WorkQueueException) {
            // Incluir información adicional específica para cada tipo de excepción
            if (exception instanceof ServerHighLoadException) {
                ServerHighLoadException e = (ServerHighLoadException) exception;
                response.put("availablePermits", e.getAvailablePermits());
                response.put("suggestedRetryAfterSeconds", 5);
            } else if (exception instanceof ProcessingTimeoutException) {
                ProcessingTimeoutException e = (ProcessingTimeoutException) exception;
                response.put("taskId", e.getTaskId());
                response.put("processingDurationMs", e.getProcessingDuration().toMillis());
            } else if (exception instanceof TaskNotFoundException) {
                TaskNotFoundException e = (TaskNotFoundException) exception;
                response.put("taskId", e.getTaskId());
            } else if (exception instanceof TaskProcessingException) {
                TaskProcessingException e = (TaskProcessingException) exception;
                response.put("taskId", e.getTaskId());
                response.put("taskType", e.getTaskType());
            }
        }

        return ResponseEntity.status(status).body(response);
    }
}