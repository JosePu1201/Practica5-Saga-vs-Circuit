package com.Inventory_Service.demo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.Inventory_Service.demo.dto.CircuitBreakerDemoResponseDTO;
import com.Inventory_Service.demo.dto.CircuitBreakerStatusDTO;
import com.Inventory_Service.demo.service.CircuitBreakerDemoService;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerDemoControllerTest {

    @Mock
    private CircuitBreakerDemoService circuitBreakerDemoService;

    @InjectMocks
    private CircuitBreakerDemoController controller;

    private CircuitBreakerDemoResponseDTO successResponse;
    private CircuitBreakerDemoResponseDTO fallbackResponse;

    @BeforeEach
    void setUp() {
        successResponse = CircuitBreakerDemoResponseDTO.builder()
                .circuitBreakerName("inventoryDemoCircuitBreaker")
                .circuitState("CLOSED")
                .fallbackTriggered(false)
                .build();

        fallbackResponse = CircuitBreakerDemoResponseDTO.builder()
                .circuitBreakerName("inventoryDemoCircuitBreaker")
                .circuitState("OPEN")
                .fallbackTriggered(true)
                .build();
    }

    @Test
    @DisplayName("testCircuitBreaker - Exitoso retorna HTTP 200 OK")
    void testTestCircuitBreakerSuccess() {
        when(circuitBreakerDemoService.executeDemoCall("success")).thenReturn(successResponse);

        ResponseEntity<CircuitBreakerDemoResponseDTO> response = controller.testCircuitBreaker("success");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("testCircuitBreaker - Fallback activado retorna HTTP 503 SERVICE_UNAVAILABLE")
    void testTestCircuitBreakerFallback() {
        when(circuitBreakerDemoService.executeDemoCall("fail")).thenReturn(fallbackResponse);

        ResponseEntity<CircuitBreakerDemoResponseDTO> response = controller.testCircuitBreaker("fail");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    @DisplayName("getStatus - Retorna HTTP 200 OK")
    void testGetStatus() {
        CircuitBreakerStatusDTO statusDTO = CircuitBreakerStatusDTO.builder()
                .circuitBreakerName("inventoryDemoCircuitBreaker")
                .state("CLOSED")
                .build();

        when(circuitBreakerDemoService.getCircuitBreakerStatus()).thenReturn(statusDTO);

        ResponseEntity<CircuitBreakerStatusDTO> response = controller.getStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
