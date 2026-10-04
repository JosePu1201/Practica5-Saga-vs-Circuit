package com.Order_Service.demo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.Order_Service.demo.dto.CircuitBreakerDemoResponseDTO;
import com.Order_Service.demo.dto.CircuitBreakerStatusDTO;
import com.Order_Service.demo.service.CircuitBreakerDemoService;

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
                .circuitBreakerName("orderDemoCircuitBreaker")
                .circuitState("CLOSED")
                .mode("success")
                .fallbackTriggered(false)
                .message("Success")
                .build();

        fallbackResponse = CircuitBreakerDemoResponseDTO.builder()
                .circuitBreakerName("orderDemoCircuitBreaker")
                .circuitState("OPEN")
                .mode("fail")
                .fallbackTriggered(true)
                .message("Fallback")
                .build();
    }

    @Test
    @DisplayName("testCircuitBreaker - Exitoso retorna HTTP 200 OK")
    void testTestCircuitBreakerSuccess() {
        when(circuitBreakerDemoService.executeDemoCall("success")).thenReturn(successResponse);

        ResponseEntity<CircuitBreakerDemoResponseDTO> response = controller.testCircuitBreaker("success");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("CLOSED", response.getBody().getCircuitState());
    }

    @Test
    @DisplayName("testCircuitBreaker - Fallback activado retorna HTTP 503 SERVICE_UNAVAILABLE")
    void testTestCircuitBreakerFallback() {
        when(circuitBreakerDemoService.executeDemoCall("fail")).thenReturn(fallbackResponse);

        ResponseEntity<CircuitBreakerDemoResponseDTO> response = controller.testCircuitBreaker("fail");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("OPEN", response.getBody().getCircuitState());
    }

    @Test
    @DisplayName("getStatus - Retorna HTTP 200 OK con métricas")
    void testGetStatus() {
        CircuitBreakerStatusDTO statusDTO = CircuitBreakerStatusDTO.builder()
                .circuitBreakerName("orderDemoCircuitBreaker")
                .state("CLOSED")
                .build();

        when(circuitBreakerDemoService.getCircuitBreakerStatus()).thenReturn(statusDTO);

        ResponseEntity<CircuitBreakerStatusDTO> response = controller.getStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CLOSED", response.getBody().getState());
    }
}
