package co.g3a.high_throughput_poc.consumer;

import co.g3a.high_throughput_poc.worker.WorkQueueService;
import co.g3a.high_throughput_poc.worker.WorkerEnabledController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController extends WorkerEnabledController {

    @Autowired
    public ProductController(WorkQueueService workQueueService) {
        super(workQueueService);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        return enqueueTask("GET_ALL_PRODUCTS", null);
    }

    @GetMapping("/paged")
    public ResponseEntity<Map<String, Object>> getAllProductsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return enqueueTask("GET_PAGED_PRODUCTS", Map.of("page", page, "size", size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable Long id) {
        return enqueueTask("GET_PRODUCT_BY_ID", id);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getProductsByCategory(@PathVariable String category) {
        return enqueueTask("GET_PRODUCTS_BY_CATEGORY", category);
    }

    @GetMapping("/category/{category}/paged")
    public ResponseEntity<Map<String, Object>> getProductsByCategoryPaged(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return enqueueTask("GET_PRODUCTS_BY_CATEGORY_PAGED", 
                Map.of("category", category, "page", page, "size", size));
    }

    @GetMapping("/price")
    public ResponseEntity<Map<String, Object>> getProductsByPriceRange(
            @RequestParam Double min,
            @RequestParam Double max) {
        
        return enqueueTask("GET_PRODUCTS_BY_PRICE_RANGE", 
                Map.of("min", min, "max", max));
    }

    @GetMapping("/price/paged")
    public ResponseEntity<Map<String, Object>> getProductsByPriceRangePaged(
            @RequestParam Double min,
            @RequestParam Double max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return enqueueTask("GET_PRODUCTS_BY_PRICE_RANGE_PAGED", 
                Map.of("min", min, "max", max, "page", page, "size", size));
    }

    @GetMapping("/stock")
    public ResponseEntity<Map<String, Object>> getProductsByMinStock(@RequestParam Integer min) {
        return enqueueTask("GET_PRODUCTS_BY_MIN_STOCK", min);
    }

    @GetMapping("/stock/paged")
    public ResponseEntity<Map<String, Object>> getProductsByMinStockPaged(
            @RequestParam Integer min,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return enqueueTask("GET_PRODUCTS_BY_MIN_STOCK_PAGED", 
                Map.of("min", min, "page", page, "size", size));
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(@RequestParam String keyword) {
        return enqueueTask("SEARCH_PRODUCTS", keyword);
    }

    @GetMapping("/search/paged")
    public ResponseEntity<Map<String, Object>> searchProductsPaged(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return enqueueTask("SEARCH_PRODUCTS_PAGED", 
                Map.of("keyword", keyword, "page", page, "size", size));
    }
}