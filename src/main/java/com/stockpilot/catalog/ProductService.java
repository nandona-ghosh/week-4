package com.stockpilot.catalog;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly = true)
class ProductService {
    private final ProductRepository repository;
    ProductService(ProductRepository repository) { this.repository = repository; }
    List<Product> findAll() { return repository.findAll(Sort.by("id")); }
    Product find(Long id) { return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id)); }
    @Transactional Product create(ProductRequest request) {
        if (repository.existsBySku(request.sku().trim())) throw new DuplicateSkuException();
        return repository.saveAndFlush(new Product(request));
    }
    @Transactional Product update(Long id, ProductRequest request) {
        Product product = find(id);
        if (repository.existsBySkuAndIdNot(request.sku().trim(), id)) throw new DuplicateSkuException();
        product.apply(request); return repository.saveAndFlush(product);
    }
    @Transactional void delete(Long id) { Product product = find(id); repository.delete(product); repository.flush(); }
}
class ProductNotFoundException extends RuntimeException {
    ProductNotFoundException(Long id) { super("Product " + id + " was not found"); }
}
class DuplicateSkuException extends RuntimeException {
    DuplicateSkuException() { super("A product with this SKU already exists"); }
}