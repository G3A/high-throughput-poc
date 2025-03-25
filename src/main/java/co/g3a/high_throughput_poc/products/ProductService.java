package co.g3a.high_throughput_poc.products;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Page<Product>> getAllProductsAsync(int page, int size) {
        return CompletableFuture.completedFuture(
                productRepository.findAll(PageRequest.of(page, size))
        );
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<Product>> getProductByIdAsync(Long id) {
        return CompletableFuture.completedFuture(productRepository.findById(id));
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Page<Product>> getProductsByCategoryAsync(String category, int page, int size) {
        return CompletableFuture.completedFuture(
                productRepository.findByCategory(category, PageRequest.of(page, size))
        );
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Page<Product>> getProductsByCategoryAsync(String category) {
        return getProductsByCategoryAsync(category, 0, DEFAULT_PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        return productRepository.findByPriceRange(minPrice, maxPrice);
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Page<Product>> getProductsByPriceRangeAsync(Double minPrice, Double maxPrice, int page, int size) {
        return CompletableFuture.completedFuture(
                productRepository.findByPriceRange(minPrice, maxPrice, PageRequest.of(page, size))
        );
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Page<Product>> getProductsByPriceRangeAsync(Double minPrice, Double maxPrice) {
        return getProductsByPriceRangeAsync(minPrice, maxPrice, 0, DEFAULT_PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByStockGreaterThan(Integer minStock) {
        return productRepository.findByStockGreaterThan(minStock);
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Page<Product>> getProductsByStockGreaterThanAsync(Integer minStock, int page, int size) {
        return CompletableFuture.completedFuture(
                productRepository.findByStockGreaterThan(minStock, PageRequest.of(page, size))
        );
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Page<Product>> getProductsByStockGreaterThanAsync(Integer minStock) {
        return getProductsByStockGreaterThanAsync(minStock, 0, DEFAULT_PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        return productRepository.searchByKeyword(keyword);
    }

    @Async
    public CompletableFuture<List<Product>> searchProductsAsync(String keyword) {
        return CompletableFuture.completedFuture(searchProducts(keyword));
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Page<Product>> searchProductsPaginatedAsync(String keyword, int page, int size) {
        return CompletableFuture.completedFuture(
                productRepository.searchByKeyword(keyword, PageRequest.of(page, size))
        );
    }
}