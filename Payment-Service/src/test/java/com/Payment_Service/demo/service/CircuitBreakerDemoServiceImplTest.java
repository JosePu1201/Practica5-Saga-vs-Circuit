package com.Payment_Service.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.Payment_Service.demo.client.InventoryDemoClient;
import com.Payment_Service.demo.dto.CircuitBreakerDemoResponseDTO;
import com.Payment_Service.demo.dto.CircuitBreakerStatusDTO;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerDemoServiceImplTest {

    @Mock
    private CircuitBreaker paymentDemoCircuitBreaker;

    @Mock
    private InventoryDemoClient inventoryDemoClient;

    @Mock
    private CircuitBreaker.Metrics metrics;

    @Mock
    private CircuitBreakerConfig config;

    @InjectMocks
    private CircuitBreakerDemoServiceImpl service;

    @Test
    @DisplayName("executeDemoCall - Parámetro nulo o modo inválido")
    void testInvalidMode() {
        assertThrows(IllegalArgumentException.class, () -> service.executeDemoCall(null));
        assertThrows(IllegalArgumentException.class, () -> service.executeDemoCall("invalid"));
    }

    @Test
    @DisplayName("executeDemoCall - Exitoso sin Fallback")
    void testExecuteSuccess() {
        when(paymentDemoCircuitBreaker.executeSupplier(any())).thenReturn("{\"status\":\"ok\"}");
        when(paymentDemoCircuitBreaker.getName()).thenReturn("paymentDemoCircuitBreaker");
        when(paymentDemoCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        CircuitBreakerDemoResponseDTO response = service.executeDemoCall("success");

        assertNotNull(response);
        assertFalse(response.isFallbackTriggered());
    }

    @Test
    @DisplayName("executeDemoCall - CallNotPermittedException (Circuit OPEN)")
    void testExecuteCallNotPermitted() {
        when(paymentDemoCircuitBreaker.getCircuitBreakerConfig()).thenReturn(config);
        when(config.isWritableStackTraceEnabled()).thenReturn(true);
        CallNotPermittedException ex = CallNotPermittedException.createCallNotPermittedException(paymentDemoCircuitBreaker);

        when(paymentDemoCircuitBreaker.executeSupplier(any())).thenThrow(ex);
        when(paymentDemoCircuitBreaker.getName()).thenReturn("paymentDemoCircuitBreaker");
        when(paymentDemoCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.OPEN);

        CircuitBreakerDemoResponseDTO response = service.executeDemoCall("success");

        assertTrue(response.isFallbackTriggered());
    }

    @Test
    @DisplayName("executeDemoCall - RestClientResponseException")
    void testExecuteRestClientResponseException() {
        RestClientResponseException ex503 = new RestClientResponseException("503", HttpStatusCode.valueOf(503), "503", null, null, null);
        when(paymentDemoCircuitBreaker.executeSupplier(any())).thenThrow(ex503);
        when(paymentDemoCircuitBreaker.getName()).thenReturn("paymentDemoCircuitBreaker");
        when(paymentDemoCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        CircuitBreakerDemoResponseDTO response1 = service.executeDemoCall("fail");
        assertTrue(response1.isFallbackTriggered());
    }

    @Test
    @DisplayName("executeDemoCall - ResourceAccessException")
    void testExecuteResourceAccessException() {
        ResourceAccessException exTimeout = new ResourceAccessException("Timeout");
        when(paymentDemoCircuitBreaker.executeSupplier(any())).thenThrow(exTimeout);
        when(paymentDemoCircuitBreaker.getName()).thenReturn("paymentDemoCircuitBreaker");
        when(paymentDemoCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        CircuitBreakerDemoResponseDTO response2 = service.executeDemoCall("slow");
        assertTrue(response2.isFallbackTriggered());
    }

    @Test
    @DisplayName("getCircuitBreakerStatus - Retorna status completo")
    void testGetCircuitBreakerStatus() {
        when(paymentDemoCircuitBreaker.getMetrics()).thenReturn(metrics);
        when(paymentDemoCircuitBreaker.getName()).thenReturn("paymentDemoCircuitBreaker");
        when(paymentDemoCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(paymentDemoCircuitBreaker.getCircuitBreakerConfig()).thenReturn(config);
        when(config.getSlidingWindowSize()).thenReturn(10);
        when(config.getMinimumNumberOfCalls()).thenReturn(10);
        when(config.getFailureRateThreshold()).thenReturn(50.0f);
        when(config.getPermittedNumberOfCallsInHalfOpenState()).thenReturn(3);

        CircuitBreakerStatusDTO status = service.getCircuitBreakerStatus();

        assertNotNull(status);
        assertEquals("paymentDemoCircuitBreaker", status.getCircuitBreakerName());
    }
}
