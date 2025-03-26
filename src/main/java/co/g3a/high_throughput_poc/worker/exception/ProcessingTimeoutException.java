package co.g3a.high_throughput_poc.worker.exception;

import java.time.Duration;
import java.util.UUID;

// Excepción para tiempo de procesamiento excedido
public class ProcessingTimeoutException extends WorkQueueException {
    private final UUID taskId;
    private final Duration processingDuration;
    
    public ProcessingTimeoutException(UUID taskId, Duration processingDuration) {
        super("Task processing exceeded maximum allowed time: " + processingDuration);
        this.taskId = taskId;
        this.processingDuration = processingDuration;
    }
    
    public UUID getTaskId() {
        return taskId;
    }
    
    public Duration getProcessingDuration() {
        return processingDuration;
    }
}