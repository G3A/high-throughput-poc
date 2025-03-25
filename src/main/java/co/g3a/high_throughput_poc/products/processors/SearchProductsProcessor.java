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
public class SearchProductsProcessor implements TaskProcessor<String, Map<String, Object>> {
    private final ProductService productService;

    @Autowired
    public SearchProductsProcessor(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public String getTaskType() {
        return "SEARCH_PRODUCTS";
    }

    @Override
    public Map<String, Object> processTask(String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        
        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("products", products);
        result.put("count", products.size());
        
        return result;
    }
}