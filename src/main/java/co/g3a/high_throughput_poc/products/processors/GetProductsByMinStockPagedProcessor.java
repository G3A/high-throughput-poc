package co.g3a.high_throughput_poc.products.processors;

import co.g3a.high_throughput_poc.products.ProductService;
import co.g3a.high_throughput_poc.worker.TaskProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GetProductsByMinStockPagedProcessor implements TaskProcessor<Map<String, Object>, Map<String, Object>> {
    private final ProductService productService;

    @Autowired
    public GetProductsByMinStockPagedProcessor(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public String getTaskType() {
        return "GET_PRODUCTS_BY_MIN_STOCK_PAGED";
    }

    @Override
    public Map<String, Object> processTask(Map<String, Object> request) {
        Integer min = (Integer) request.get("min");
        int page = (Integer) request.get("page");
        int size = (Integer) request.get("size");
        
        Page<?> products = productService.getProductsByStockGreaterThanAsync(min, page, size).join();
        
        Map<String, Object> result = new HashMap<>();
        result.put("minStock", min);
        result.put("products", products.getContent());
        result.put("totalPages", products.getTotalPages());
        result.put("totalElements", products.getTotalElements());
        result.put("currentPage", products.getNumber());
        
        return result;
    }
}