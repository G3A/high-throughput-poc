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
public class GetAllProductsProcessor implements TaskProcessor<Void, Map<String, Object>> {

    private final ProductService productService;

    @Autowired
    public GetAllProductsProcessor(ProductService productService) {
        this.productService = productService;
    }
    
    @Override
    public String getTaskType() {
        return "GET_ALL_PRODUCTS";
    }

    @Override
    public Map<String, Object> processTask(Void payload) {
        List<Product> products = productService.getAllProducts();
        
        Map<String, Object> result = new HashMap<>();
        result.put("products", products);
        result.put("count", products.size());
        
        return result;
    }
}