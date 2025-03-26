package co.g3a.high_throughput_poc.worker.exception;

// Excepción para alta carga del servidor
public class ServerHighLoadException extends WorkQueueException {
    private final int availablePermits;
    
    public ServerHighLoadException(int availablePermits) {
        super("Server is currently at high load. Please try again later.");
        this.availablePermits = availablePermits;
    }
    
    public int getAvailablePermits() {
        return availablePermits;
    }
}