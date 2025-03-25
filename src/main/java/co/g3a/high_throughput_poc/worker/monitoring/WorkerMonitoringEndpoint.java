package co.g3a.high_throughput_poc.worker.monitoring;

import co.g3a.high_throughput_poc.worker.WorkQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@WebEndpoint(id = "worker-stats")
public class WorkerMonitoringEndpoint {
    
    private final WorkQueueService workQueueService;
    
    @Autowired
    public WorkerMonitoringEndpoint(WorkQueueService workQueueService) {
        this.workQueueService = workQueueService;
    }
    
    @ReadOperation
    public Map<String, Object> getWorkerStats() {
        return workQueueService.getStatistics();
    }
}