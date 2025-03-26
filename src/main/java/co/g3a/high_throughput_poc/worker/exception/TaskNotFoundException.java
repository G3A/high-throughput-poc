package co.g3a.high_throughput_poc.worker.exception;

import java.util.UUID;

// Excepción para tareas no encontradas
public class TaskNotFoundException extends WorkQueueException {
    private final UUID taskId;
    
    public TaskNotFoundException(UUID taskId) {
        super("Task not found or expired: " + taskId);
        this.taskId = taskId;
    }
    
    public UUID getTaskId() {
        return taskId;
    }
}