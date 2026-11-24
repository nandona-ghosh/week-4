package com.stockpilot.catalog;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class FoodLookupIntegrationTest {
    private static final AtomicInteger requests = new AtomicInteger();
    private static final HttpServer server = startServer();
    @Autowired MockMvc mvc;
    private static HttpServer startServer() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            s.createContext("/api/v2/product/", exchange -> {
                requests.incrementAndGet();
                String path = exchange.getRequestURI().getPath();
                String agent = exchange.getRequestHeaders().getFirst("User-Agent");
                String query = exchange.getRequestURI().getRawQuery();
                int status = 200;
                String body;
                if (!"StockPilot-Test/1.0 (https://example.com)".equals(agent) || query == null || !query.contains("fields=")) { status=400; body="{}"; }
                else if (path.contains("11111111")) body="{\"status\":0}";
                else if (path.contains("22222222")) { status=503; body="{}"; }
                else if (path.contains("33333333")) body="{bad json";
                else if (path.contains("55555555")) { try { Thread.sleep(1200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } body="{}"; }
                else if (path.contains("44444444")) body="{\"status\":1,\"product\":{}}";
                else body="{\"status\":1,\"product\":{\"product_name\":\"Chocolate spread\",\"brands\":\"Example\",\"quantity\":\"400 g\",\"unused\":true}}";
                byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                try (var output=exchange.getResponseBody()) { output.write(bytes); }
            });
            s.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(task -> { Thread t = new Thread(task); t.setDaemon(true); return t; }));
            s.start(); return s;
        } catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("stockpilot.food-api.base-url", () -> "http://127.0.0.1:" + server.getAddress().getPort());
        registry.add("stockpilot.food-api.user-agent", () -> "StockPilot-Test/1.0 (https://example.com)");
        registry.add("stockpilot.food-api.timeout-ms", () -> "300");
    }
    @AfterAll static void stopServer() { server.stop(0); }
    @Test void mapsProviderProductAndSendsExpectedRequest() throws Exception {
        mvc.perform(get("/api/v1/food-products/3017620422003"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.barcode").value("3017620422003"))
            .andExpect(jsonPath("$.name").value("Chocolate spread"))
            .andExpect(jsonPath("$.brands").value("Example")).andExpect(jsonPath("$.quantity").value("400 g"));
    }
    @Test void missingProductReturns404() throws Exception {
        mvc.perform(get("/api/v1/food-products/11111111")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("FOOD_NOT_FOUND"));
    }
    @Test void upstreamFailureAndInvalidResponsesReturn502() throws Exception {
        for (String code : new String[]{"22222222", "33333333", "44444444"})
            mvc.perform(get("/api/v1/food-products/"+code)).andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("FOOD_PROVIDER_ERROR"));
    }
    @Test void timeoutReturns502() throws Exception {
        mvc.perform(get("/api/v1/food-products/55555555")).andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("FOOD_PROVIDER_ERROR"));
    }
    @Test void invalidBarcodeIsRejectedBeforeNetworkCall() throws Exception {
        int before = requests.get();
        mvc.perform(get("/api/v1/food-products/not-a-barcode")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_BARCODE"));
        assertEquals(before, requests.get());
    }
}
