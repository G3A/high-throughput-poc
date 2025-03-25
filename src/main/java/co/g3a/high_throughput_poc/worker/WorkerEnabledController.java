package co.g3a.high_throughput_poc.worker;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    protected ResponseEntity<SseEmitter> enqueueTaskAsync(String taskType, Object payload) {
        // Encolar la tarea para procesamiento inmediato
        UUID taskId = workQueueService.enqueueTask(taskType, payload);
        
        // Crear un emitter para esta tarea
        SseEmitter emitter = workQueueService.createEmitterForTask(taskId);
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }
    
    /**
     * Procesa una tarea y espera su resultado (síncrono)
     */
    protected ResponseEntity<Map<String, Object>> enqueueTask(String taskType, Object payload) {
        // Procesar la tarea y obtener el resultado de forma síncrona
        Map<String, Object> result = workQueueService.processTaskAndWaitResult(taskType, payload);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Encola una tarea y devuelve inmediatamente el ID de la tarea (asíncrono sin SSE)
     */
    protected ResponseEntity<Map<String, Object>> enqueueTaskAndReturnId(String taskType, Object payload) {
        // Encolar la tarea para procesamiento en segundo plano
        UUID taskId = workQueueService.enqueueTask(taskType, payload);
        
        // Devolver inmediatamente el ID de la tarea y su estado inicial
        Map<String, Object> response = new HashMap<>();
        response.put("idTask", taskId);
        response.put("status", "ACCEPTED");
        
        return ResponseEntity.accepted().body(response);
    }
}