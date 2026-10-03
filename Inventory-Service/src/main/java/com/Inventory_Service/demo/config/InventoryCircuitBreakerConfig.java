package com.Inventory_Service.demo.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@Configuration
public class InventoryCircuitBreakerConfig {

    public static final String CIRCUIT_BREAKER_NAME = "inventoryDemoCircuitBreaker";

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(
                        RestClientResponseException.class,
                        ResourceAccessException.class,
                        java.io.IOException.class,
                        java.util.concurrent.TimeoutException.class)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public CircuitBreaker inventoryDemoCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(CIRCUIT_BREAKER_NAME);
    }

    @Bean
    public RestClient circuitBreakerDemoRestClient(
            @Value("${circuitbreaker.demo.connect-timeout-ms:2000}") int connectTimeout,
            @Value("${circuitbreaker.demo.read-timeout-ms:2000}") int readTimeout) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}