package co.g3a.high_throughput_poc.old_solution;

import co.g3a.high_throughput_poc.products.Product;
import co.g3a.high_throughput_poc.products.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

//@RestController
//@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/paged")
    public CompletableFuture<ResponseEntity<Page<Product>>> getAllProductsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getAllProductsAsync(page, size)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/async/{id}")
    public CompletableFuture<ResponseEntity<Product>> getProductByIdAsync(@PathVariable Long id) {
        return productService.getProductByIdAsync(id)
                .thenApply(product -> product
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    @GetMapping("/category/{category}/paged")
    public CompletableFuture<ResponseEntity<Page<Product>>> getProductsByCategoryPaged(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getProductsByCategoryAsync(category, page, size)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/price")
    public ResponseEntity<List<Product>> getProductsByPriceRange(
            @RequestParam Double min,
            @RequestParam Double max) {
        return ResponseEntity.ok(productService.getProductsByPriceRange(min, max));
    }

    @GetMapping("/price/paged")
    public CompletableFuture<ResponseEntity<Page<Product>>> getProductsByPriceRangePaged(
            @RequestParam Double min,
            @RequestParam Double max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getProductsByPriceRangeAsync(min, max, page, size)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/stock")
    public ResponseEntity<List<Product>> getProductsByMinStock(@RequestParam Integer min) {
        return ResponseEntity.ok(productService.getProductsByStockGreaterThan(min));
    }

    @GetMapping("/stock/paged")
    public CompletableFuture<ResponseEntity<Page<Product>>> getProductsByMinStockPaged(
            @RequestParam Integer min,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getProductsByStockGreaterThanAsync(min, page, size)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @GetMapping("/search/async")
    public CompletableFuture<ResponseEntity<List<Product>>> searchProductsAsync(@RequestParam String keyword) {
        return productService.searchProductsAsync(keyword)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/search/paged")
    public CompletableFuture<ResponseEntity<Page<Product>>> searchProductsPaged(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.searchProductsPaginatedAsync(keyword, page, size)
                .thenApply(ResponseEntity::ok);
    }
}