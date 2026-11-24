package com.stockpilot.catalog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class FoodLookupService {
    private final FoodGateway gateway;
    private final long ttlNanos;
    private final int capacity;
    private final LongSupplier ticker;
    private final Map<String, Entry> cache = new LinkedHashMap<>(16, 0.75f, true);
    private record Entry(FoodProduct product, long createdAt) {}

    @Autowired
    FoodLookupService(FoodGateway gateway,
            @Value("${stockpilot.food-cache.ttl-seconds:300}") long ttlSeconds,
            @Value("${stockpilot.food-cache.max-entries:500}") int capacity) {
        this(gateway, ttlSeconds, capacity, System::nanoTime);
    }
    FoodLookupService(FoodGateway gateway) { this(gateway, 300, 500, System::nanoTime); }
    FoodLookupService(FoodGateway gateway, long ttlSeconds, int capacity, LongSupplier ticker) {
        if (ttlSeconds < 0 || ttlSeconds > 86400 || capacity < 0 || capacity > 100000)
            throw new IllegalArgumentException("Invalid food cache configuration");
        this.gateway = gateway;
        this.ttlNanos = java.util.concurrent.TimeUnit.SECONDS.toNanos(ttlSeconds);
        this.capacity = capacity;
        this.ticker = ticker;
    }
    FoodProduct lookup(String barcode) {
        if (barcode == null || !barcode.matches("[0-9]{8,14}")) throw new InvalidBarcodeException();
        synchronized (cache) {
            Entry entry = cache.get(barcode);
            if (entry != null) {
                if (ticker.getAsLong() - entry.createdAt() < ttlNanos) return entry.product();
                cache.remove(barcode);
            }
        }
        // Network calls are outside the lock; simultaneous misses may call the provider more than once.
        FoodProduct product = gateway.lookup(barcode);
        if (ttlNanos > 0 && capacity > 0) synchronized (cache) {
            cache.put(barcode, new Entry(product, ticker.getAsLong()));
            while (cache.size() > capacity) cache.remove(cache.keySet().iterator().next());
        }
        return product;
    }
}
class InvalidBarcodeException extends RuntimeException {
    InvalidBarcodeException() { super("Barcode must contain 8 to 14 digits"); }
}
class FoodNotFoundException extends RuntimeException {
    FoodNotFoundException() { super("No food product found for this barcode"); }
}
class FoodProviderException extends RuntimeException {
    FoodProviderException() { super("Food product service is unavailable or returned an invalid response"); }
}
