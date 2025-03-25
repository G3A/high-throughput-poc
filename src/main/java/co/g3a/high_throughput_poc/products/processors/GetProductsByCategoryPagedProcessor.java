package co.g3a.high_throughput_poc.products.processors;

import co.g3a.high_throughput_poc.products.ProductService;
import co.g3a.high_throughput_poc.worker.TaskProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GetProductsByCategoryPagedProcessor implements TaskProcessor<Map<String, Object>, Map<String, Object>> {
    private final ProductService productService;

    @Autowired
    public GetProductsByCategoryPagedProcessor(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public String getTaskType() {
        return "GET_PRODUCTS_BY_CATEGORY_PAGED";
    }

    @Override
    public Map<String, Object> processTask(Map<String, Object> request) {
        String category = (String) request.get("category");
        int page = (Integer) request.get("page");
        int size = (Integer) request.get("size");
        
        Page<?> products = productService.getProductsByCategoryAsync(category, page, size).join();
        
        Map<String, Object> result = new HashMap<>();
        result.put("category", category);
        result.put("products", products.getContent());
        result.put("totalPages", products.getTotalPages());
        result.put("totalElements", products.getTotalElements());
        result.put("currentPage", products.getNumber());
        
        return result;
    }
}