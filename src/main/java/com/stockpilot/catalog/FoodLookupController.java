package com.stockpilot.catalog;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/food-products")
class FoodLookupController {
    private final FoodLookupService service;
    FoodLookupController(FoodLookupService service) { this.service = service; }
    @GetMapping("/{barcode}") FoodProduct lookup(@PathVariable String barcode) { return service.lookup(barcode); }
}
