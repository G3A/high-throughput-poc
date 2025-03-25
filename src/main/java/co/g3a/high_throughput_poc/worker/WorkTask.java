package co.g3a.high_throughput_poc.worker;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class WorkTask<T, R> {
    public enum TaskStatus {
        PENDING,
        PROCESSED,
        REJECTED
    }
    
    private final UUID id;
    private final String type;
    private final T request;
    private final Duration maxProcessingTime;
    private final Instant createdAt;
    private Instant processedAt;
    private R result;
    private TaskStatus status;
    
    public WorkTask(String type, T request, Duration maxProcessingTime) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.request = request;
        this.maxProcessingTime = maxProcessingTime;
        this.createdAt = Instant.now();
        this.status = TaskStatus.PENDING;
    }
    
    public UUID getId() {
        return id;
    }
    
    public String getType() {
        return type;
    }
    
    public T getRequest() {
        return request;
    }
    
    public Duration getMaxProcessingTime() {
        return maxProcessingTime;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
    
    public R getResult() {
        return result;
    }
    
    public void setResult(R result) {
        this.result = result;
        this.processedAt = Instant.now();
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public Duration getProcessingDuration() {
        if (processedAt == null) {
            return Duration.between(createdAt, Instant.now());
        }
        return Duration.between(createdAt, processedAt);
    }
}