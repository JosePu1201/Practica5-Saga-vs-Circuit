package com.Inventory_Service.demo.service;

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

import com.Inventory_Service.demo.client.ShippingDemoClient;
import com.Inventory_Service.demo.dto.CircuitBreakerDemoResponseDTO;
import com.Inventory_Service.demo.dto.CircuitBreakerStatusDTO;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerDemoServiceImplTest {

    @Mock
    private CircuitBreaker inventoryDemoCircuitBreaker;

    @Mock
    private ShippingDemoClient shippingDemoClient;

    @Mock
    private CircuitBreaker.Metrics metrics;

    @Mock
    private CircuitBreakerConfig config;

    @InjectMocks
    private CircuitBreakerDemoServiceImpl service;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("executeDemoCall - Parámetro mode nulo o vacío debe lanzar IllegalArgumentException")
    void testNullOrEmptyMode() {
        assertThrows(IllegalArgumentException.class, () -> service.executeDemoCall(null));
        assertThrows(IllegalArgumentException.class, () -> service.executeDemoCall("   "));
    }

    @Test
    @DisplayName("executeDemoCall - Modo inválido debe lanzar IllegalArgumentException")
    void testInvalidMode() {
        assertThrows(IllegalArgumentException.class, () -> service.executeDemoCall("invalid_mode"));
    }

    @Test
    @DisplayName("executeDemoCall - Exitoso sin activarse Fallback")
    void testExecuteSuccess() {
        when(inventoryDemoCircuitBreaker.executeSupplier(any())).thenReturn("{\"status\":\"ok\"}");
        when(inventoryDemoCircuitBreaker.getName()).thenReturn("inventoryDemoCircuitBreaker");
        when(inventoryDemoCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        CircuitBreakerDemoResponseDTO response = service.executeDemoCall("success");

        assertNotNull(response);
        assertFalse(response.isFallbackTriggered());
        assertEquals("CLOSED", response.getCircuitState());
    }

    @Test
    @DisplayName("executeDemoCall - Atrapa CallNotPermittedException cuando está OPEN")
    void testExecuteCallNotPermitted() {
        when(inventoryDemoCircuitBreaker.getCircuitBreakerConfig()).thenReturn(config);
        when(config.isWritableStackTraceEnabled()).thenReturn(true);
        CallNotPermittedException ex = CallNotPermittedException.createCallNotPermittedException(inventoryDemoCircuitBreaker);

        when(inventoryDemoCircuitBreaker.executeSupplier(any())).thenThrow(ex);
        when(inventoryDemoCircuitBreaker.getName()).thenReturn("inventoryDemoCircuitBreaker");
        when(inventoryDemoCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.OPEN);

        CircuitBreakerDemoResponseDTO response = service.executeDemoCall("success");

        assertTrue(response.isFallbackTriggered());
        assertEquals("OPEN", response.getCircuitState());
    }

    @Test
    @DisplayName("executeDemoCall - Atrapa RestClientResponseException cuando la dependencia remota falla")
    void testExecuteRestClientResponseException() {
        RestClientResponseException ex = new RestClientResponseException("Service Unavailable", HttpStatusCode.valueOf(503), "503", null, null, null);
        when(inventoryDemoCircuitBreaker.executeSupplier(any())).thenThrow(ex);
        when(inventoryDemoCircuitBreaker.getName()).thenReturn("inventoryDemoCircuitBreaker");
        when(inventoryDemoCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        CircuitBreakerDemoResponseDTO response = service.executeDemoCall("fail");

        assertTrue(response.isFallbackTriggered());
    }

    @Test
    @DisplayName("executeDemoCall - Atrapa ResourceAccessException (Timeout)")
    void testExecuteResourceAccessException() {
        ResourceAccessException ex = new ResourceAccessException("Read timed out");
        when(inventoryDemoCircuitBreaker.executeSupplier(any())).thenThrow(ex);
        when(inventoryDemoCircuitBreaker.getName()).thenReturn("inventoryDemoCircuitBreaker");
        when(inventoryDemoCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        CircuitBreakerDemoResponseDTO response = service.executeDemoCall("slow");

        assertTrue(response.isFallbackTriggered());
    }

    @Test
    @DisplayName("getCircuitBreakerStatus - Retorna status y métricas completas")
    void testGetCircuitBreakerStatus() {
        when(inventoryDemoCircuitBreaker.getMetrics()).thenReturn(metrics);
        when(inventoryDemoCircuitBreaker.getName()).thenReturn("inventoryDemoCircuitBreaker");
        when(inventoryDemoCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(inventoryDemoCircuitBreaker.getCircuitBreakerConfig()).thenReturn(config);
        when(config.getSlidingWindowSize()).thenReturn(10);
        when(config.getMinimumNumberOfCalls()).thenReturn(10);
        when(config.getFailureRateThreshold()).thenReturn(50.0f);
        when(config.getPermittedNumberOfCallsInHalfOpenState()).thenReturn(3);

        CircuitBreakerStatusDTO status = service.getCircuitBreakerStatus();

        assertNotNull(status);
        assertEquals("inventoryDemoCircuitBreaker", status.getCircuitBreakerName());
        assertEquals("CLOSED", status.getState());
    }
}
