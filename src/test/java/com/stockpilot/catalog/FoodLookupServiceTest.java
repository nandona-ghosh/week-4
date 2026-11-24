package com.stockpilot.catalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class FoodLookupServiceTest {
    private final FoodGateway gateway = mock(FoodGateway.class);
    private final FoodLookupService service = new FoodLookupService(gateway);
    @Test void returnsGatewayProduct() {
        FoodProduct expected = new FoodProduct("3017620422003", "Chocolate spread", "Example", "400 g");
        when(gateway.lookup("3017620422003")).thenReturn(expected);
        assertEquals(expected, service.lookup("3017620422003"));
        verify(gateway).lookup("3017620422003");
    }
    @Test void rejectsInvalidBarcodesWithoutCallingProvider() {
        for (String value : new String[]{"abc12345", "123", "123456789012345", "1234-5678", ""})
            assertThrows(InvalidBarcodeException.class, () -> service.lookup(value));
        assertThrows(InvalidBarcodeException.class, () -> service.lookup(null));
        verifyNoInteractions(gateway);
    }
    @Test void propagatesMissingAndUnavailableProviderResults() {
        when(gateway.lookup("12345678")).thenThrow(new FoodNotFoundException());
        when(gateway.lookup("87654321")).thenThrow(new FoodProviderException());
        assertThrows(FoodNotFoundException.class, () -> service.lookup("12345678"));
        assertThrows(FoodProviderException.class, () -> service.lookup("87654321"));
    }
}
