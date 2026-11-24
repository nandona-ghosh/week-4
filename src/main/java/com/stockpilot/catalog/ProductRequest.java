package com.stockpilot.catalog;
import jakarta.validation.constraints.*;
public record ProductRequest(
    @NotBlank @Size(max=64) String sku,
    @NotBlank @Size(max=200) String name,
    @NotBlank @Size(max=32) String unit,
    @NotNull @Min(0) Integer reorderPoint
) {}