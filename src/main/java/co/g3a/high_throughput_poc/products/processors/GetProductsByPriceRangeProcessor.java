package co.g3a.high_throughput_poc.products.processors;

import co.g3a.high_throughput_poc.products.Product;
import co.g3a.high_throughput_poc.products.ProductService;
import co.g3a.high_throughput_poc.worker.TaskProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetProductsByPriceRangeProcessor implements TaskProcessor<Map<String, Double>, Map<String, Object>> {
    private final ProductService productService;

    @Autowired
    public GetProductsByPriceRangeProcessor(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public String getTaskType() {
        return "GET_PRODUCTS_BY_PRICE_RANGE";
    }

    @Override
    public Map<String, Object> processTask(Map<String, Double> request) {
        Double min = request.get("min");
        Double max = request.get("max");
        List<Product> products = productService.getProductsByPriceRange(min, max);
        
        Map<String, Object> result = new HashMap<>();
        result.put("minPrice", min);
        result.put("maxPrice", max);
        result.put("products", products);
        result.put("count", products.size());
        
        return result;
    }
}