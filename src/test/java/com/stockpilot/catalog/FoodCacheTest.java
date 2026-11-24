package com.stockpilot.catalog;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class FoodCacheTest {
    @Test void cachesSuccessExpiresAndEvictsLeastRecentlyUsed() {
        FoodGateway gateway = mock(FoodGateway.class);
        when(gateway.lookup(anyString())).thenAnswer(i -> new FoodProduct(i.getArgument(0), "Name", null, null));
        AtomicLong time = new AtomicLong();
        FoodLookupService service = new FoodLookupService(gateway, 10, 2, time::get);
        service.lookup("11111111"); service.lookup("22222222"); service.lookup("11111111");
        service.lookup("33333333"); service.lookup("22222222");
        verify(gateway, times(1)).lookup("11111111");
        verify(gateway, times(2)).lookup("22222222");
        time.set(10_000_000_000L);
        service.lookup("22222222");
        verify(gateway, times(3)).lookup("22222222");
    }
    @Test void failuresAreNeverCachedAndZeroDisablesCache() {
        FoodGateway gateway = mock(FoodGateway.class);
        when(gateway.lookup("11111111")).thenThrow(new FoodProviderException());
        FoodLookupService service = new FoodLookupService(gateway, 10, 2, System::nanoTime);
        assertThrows(FoodProviderException.class, () -> service.lookup("11111111"));
        assertThrows(FoodProviderException.class, () -> service.lookup("11111111"));
        verify(gateway, times(2)).lookup("11111111");
        when(gateway.lookup("22222222")).thenReturn(new FoodProduct("22222222", "Name", null, null));
        FoodLookupService disabled = new FoodLookupService(gateway, 0, 2, System::nanoTime);
        disabled.lookup("22222222"); disabled.lookup("22222222");
        verify(gateway, times(2)).lookup("22222222");
    }
}
