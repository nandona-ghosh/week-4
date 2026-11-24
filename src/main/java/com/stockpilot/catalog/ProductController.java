package com.stockpilot.catalog;
import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
@RestController
@RequestMapping("/api/v1/products")
class ProductController {
    private final ProductService service;
    ProductController(ProductService service) { this.service = service; }
    @GetMapping List<Product> list() { return service.findAll(); }
    @GetMapping("/{id}") Product get(@PathVariable Long id) { return service.find(id); }
    @PostMapping ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        Product product = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(product.getId()).toUri();
        return ResponseEntity.created(location).body(product);
    }
    @PutMapping("/{id}") Product update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}