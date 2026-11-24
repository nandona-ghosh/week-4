package com.stockpilot.catalog;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FoodBenchmarkTest {
    @Test void compareRepeatedLookups() {
        run(0); run(300); // warm up both paths
        long[] before = run(0), after = run(300);
        System.out.printf("BENCHMARK: requests=50 provider_delay_ms=20 baseline_ms=%.3f cached_ms=%.3f baseline_calls=%d cached_calls=%d%n", before[0]/1e6, after[0]/1e6, before[1], after[1]);
        assertEquals(50, before[1]); assertEquals(1, after[1]);
    }
    private long[] run(long ttl) {
        AtomicInteger calls = new AtomicInteger();
        FoodGateway gateway = barcode -> {
            calls.incrementAndGet();
            try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            return new FoodProduct(barcode, "Name", null, null);
        };
        FoodLookupService service = new FoodLookupService(gateway, ttl, 500, System::nanoTime);
        long start = System.nanoTime();
        for (int i = 0; i < 50; i++) service.lookup("12345678");
        return new long[]{System.nanoTime()-start, calls.get()};
    }
}
