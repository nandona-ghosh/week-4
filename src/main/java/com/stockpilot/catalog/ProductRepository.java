package com.stockpilot.catalog;
import org.springframework.data.jpa.repository.JpaRepository;
interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
}