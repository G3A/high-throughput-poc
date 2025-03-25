package co.g3a.high_throughput_poc.products.processors;

import co.g3a.high_throughput_poc.products.ProductService;
import co.g3a.high_throughput_poc.worker.TaskProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GetProductsByCategoryProcessor implements TaskProcessor<String, Map<String, Object>> {

    private final ProductService productService;

    @Autowired
    public GetProductsByCategoryProcessor(ProductService productService) {
        this.productService = productService;
    }
    
    @Override
    public String getTaskType() {
        return "GET_PRODUCTS_BY_CATEGORY";
    }

    @Override
    public Map<String, Object> processTask(String category) {
        Page<?> products = productService.getProductsByCategoryAsync(category).join();
        
        Map<String, Object> result = new HashMap<>();
        result.put("category", category);
        result.put("products", products.getContent());
        result.put("totalElements", products.getTotalElements());
        result.put("totalPages", products.getTotalPages());
        
        return result;
    }
}