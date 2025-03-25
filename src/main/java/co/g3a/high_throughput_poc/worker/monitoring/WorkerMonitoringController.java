package co.g3a.high_throughput_poc.worker.monitoring;

import co.g3a.high_throughput_poc.worker.WorkQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/worker")
public class WorkerMonitoringController {
    
    private final WorkQueueService workQueueService;
    
    @Autowired
    public WorkerMonitoringController(WorkQueueService workQueueService) {
        this.workQueueService = workQueueService;
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWorkerStatistics() {
        return ResponseEntity.ok(workQueueService.getStatistics());
    }
}