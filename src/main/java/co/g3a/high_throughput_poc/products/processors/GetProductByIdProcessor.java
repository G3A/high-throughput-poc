package co.g3a.high_throughput_poc.products.processors;

import co.g3a.high_throughput_poc.products.Product;
import co.g3a.high_throughput_poc.products.ProductService;
import co.g3a.high_throughput_poc.worker.TaskProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class GetProductByIdProcessor implements TaskProcessor<Long, Map<String, Object>> {

    private final ProductService productService;

    @Autowired
    public GetProductByIdProcessor(ProductService productService) {
        this.productService = productService;
    }
    
    @Override
    public String getTaskType() {
        return "GET_PRODUCT_BY_ID";
    }

    @Override
    public Map<String, Object> processTask(Long id) {
        Optional<Product> productOpt = productService.getProductByIdAsync(id).join();
        
        Map<String, Object> result = new HashMap<>();
        if (productOpt.isPresent()) {
            result.put("product", productOpt.get());
            result.put("found", true);
        } else {
            result.put("found", false);
            result.put("message", "Product not found with id: " + id);
        }
        
        return result;
    }
}