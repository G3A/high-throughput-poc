package co.g3a.high_throughput_poc.products.processors;

import co.g3a.high_throughput_poc.products.ProductService;
import co.g3a.high_throughput_poc.worker.TaskProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GetPagedProductsProcessor implements TaskProcessor<Map<String, Object>, Map<String, Object>> {

    private final ProductService productService;

    @Autowired
    public GetPagedProductsProcessor(ProductService productService) {
        this.productService = productService;
    }
    
    @Override
    public String getTaskType() {
        return "GET_PAGED_PRODUCTS";
    }

    @Override
    public Map<String, Object> processTask(Map<String, Object> params) {
        int page = (int) params.get("page");
        int size = (int) params.get("size");
        
        Page<?> products = productService.getAllProductsAsync(page, size).join();
        
        Map<String, Object> result = new HashMap<>();
        result.put("products", products.getContent());
        result.put("totalPages", products.getTotalPages());
        result.put("totalElements", products.getTotalElements());
        result.put("currentPage", products.getNumber());
        result.put("pageSize", products.getSize());
        
        return result;
    }
}