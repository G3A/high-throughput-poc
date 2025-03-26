package co.g3a.high_throughput_poc.worker.exception;

// Excepción para procesador no encontrado
public class ProcessorNotFoundException extends WorkQueueException {
    private final String taskType;
    
    public ProcessorNotFoundException(String taskType) {
        super("No processor registered for task type: " + taskType);
        this.taskType = taskType;
    }
    
    public String getTaskType() {
        return taskType;
    }
}