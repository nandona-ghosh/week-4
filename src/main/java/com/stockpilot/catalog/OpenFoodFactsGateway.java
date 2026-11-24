package com.stockpilot.catalog;
import java.time.Duration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
@Component
class OpenFoodFactsGateway implements FoodGateway {
    private final RestClient client;
    OpenFoodFactsGateway(RestClient.Builder builder,
            @Value("${stockpilot.food-api.base-url}") String baseUrl,
            @Value("${stockpilot.food-api.user-agent}") String userAgent,
            @Value("${stockpilot.food-api.timeout-ms:3000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        client = builder.baseUrl(baseUrl).defaultHeader("User-Agent", userAgent).requestFactory(factory).build();
    }
    @Override public FoodProduct lookup(String barcode) {
        try {
            ProviderResponse response = client.get().uri("/api/v2/product/{barcode}.json?fields=code,product_name,brands,quantity", barcode)
                    .retrieve().body(ProviderResponse.class);
            if (response == null || response.status() == null) throw new FoodProviderException();
            if (response.status() == 0) throw new FoodNotFoundException();
            if (response.status() != 1 || response.product() == null || response.product().name() == null || response.product().name().isBlank()) throw new FoodProviderException();
            return new FoodProduct(barcode, response.product().name(), response.product().brands(), response.product().quantity());
        } catch (RestClientException e) { throw new FoodProviderException(); }
    }
    @JsonIgnoreProperties(ignoreUnknown=true)
    record ProviderResponse(Integer status, ProviderProduct product) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    record ProviderProduct(@JsonProperty("product_name") String name, String brands, String quantity) {}
}
