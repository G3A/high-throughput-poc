package co.g3a.high_throughput_poc.worker.exception;

import java.util.UUID;

// Excepción para errores en el procesamiento de tareas
public class TaskProcessingException extends WorkQueueException {
    private final UUID taskId;
    private final String taskType;
    
    public TaskProcessingException(UUID taskId, String taskType, String message, Throwable cause) {
        super("Error processing task of type '" + taskType + "': " + message, cause);
        this.taskId = taskId;
        this.taskType = taskType;
    }
    
    public UUID getTaskId() {
        return taskId;
    }
    
    public String getTaskType() {
        return taskType;
    }
}