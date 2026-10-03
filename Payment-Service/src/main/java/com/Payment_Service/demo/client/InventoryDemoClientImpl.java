package com.Payment_Service.demo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InventoryDemoClientImpl implements InventoryDemoClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryDemoClientImpl.class);

    private final RestClient circuitBreakerDemoRestClient;
    private final String targetUrl;

    public InventoryDemoClientImpl(
            RestClient circuitBreakerDemoRestClient,
            @Value("${circuitbreaker.demo.target-url:http://localhost:8081/api/circuit-breaker/demo-dependency}") String targetUrl) {
        this.circuitBreakerDemoRestClient = circuitBreakerDemoRestClient;
        this.targetUrl = targetUrl;
    }

    @Override
    public String callDemoDependency(String mode) {
        String fullUrl = targetUrl.contains("?")
                ? targetUrl + "&mode={mode}"
                : targetUrl + "?mode={mode}";

        log.info("[Circuit Breaker Client] Invocando GET a {} con mode={}", targetUrl, mode);

        return circuitBreakerDemoRestClient.get()
                .uri(fullUrl, mode)
                .retrieve()
                .body(String.class);
    }
}