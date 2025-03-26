package co.g3a.high_throughput_poc.worker.exception;

import java.time.Duration;
import java.util.UUID;

// Clase base para excepciones del servicio de cola de trabajo
public class WorkQueueException extends RuntimeException {
    public WorkQueueException(String message) {
        super(message);
    }
    
    public WorkQueueException(String message, Throwable cause) {
        super(message, cause);
    }
}