package co.g3a.high_throughput_poc.products.processors;

import co.g3a.high_throughput_poc.products.ProductService;
import co.g3a.high_throughput_poc.worker.TaskProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GetProductsByPriceRangePagedProcessor implements TaskProcessor<Map<String, Object>, Map<String, Object>> {
    private final ProductService productService;

    @Autowired
    public GetProductsByPriceRangePagedProcessor(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public String getTaskType() {
        return "GET_PRODUCTS_BY_PRICE_RANGE_PAGED";
    }

    @Override
    public Map<String, Object> processTask(Map<String, Object> request) {
        Double min = (Double) request.get("min");
        Double max = (Double) request.get("max");
        int page = (Integer) request.get("page");
        int size = (Integer) request.get("size");
        
        Page<?> products = productService.getProductsByPriceRangeAsync(min, max, page, size).join();
        
        Map<String, Object> result = new HashMap<>();
        result.put("minPrice", min);
        result.put("maxPrice", max);
        result.put("products", products.getContent());
        result.put("totalPages", products.getTotalPages());
        result.put("totalElements", products.getTotalElements());
        result.put("currentPage", products.getNumber());
        
        return result;
    }
}