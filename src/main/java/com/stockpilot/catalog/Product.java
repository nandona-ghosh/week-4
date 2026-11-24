package com.stockpilot.catalog;
import jakarta.persistence.*;
@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(name = "uk_product_sku", columnNames = "sku"))
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64) private String sku;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 32) private String unit;
    @Column(nullable = false) private int reorderPoint;
    protected Product() {}
    Product(ProductRequest request) { apply(request); }
    void apply(ProductRequest request) { sku=request.sku().trim(); name=request.name().trim(); unit=request.unit().trim(); reorderPoint=request.reorderPoint(); }
    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public int getReorderPoint() { return reorderPoint; }
}