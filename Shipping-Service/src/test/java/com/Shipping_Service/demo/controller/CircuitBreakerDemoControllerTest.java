package com.Shipping_Service.demo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.Shipping_Service.demo.dto.CircuitBreakerDemoResponseDTO;
import com.Shipping_Service.demo.dto.CircuitBreakerStatusDTO;
import com.Shipping_Service.demo.service.CircuitBreakerDemoService;

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
        successResponse = CircuitBreakerDemoResponseDTO.builder().fallbackTriggered(false).build();
        fallbackResponse = CircuitBreakerDemoResponseDTO.builder().fallbackTriggered(true).build();
    }

    @Test
    @DisplayName("testCircuitBreaker - Exitoso HTTP 200 / Fallback HTTP 503")
    void testCircuitBreakerEndpoints() {
        when(circuitBreakerDemoService.executeDemoCall("success")).thenReturn(successResponse);
        when(circuitBreakerDemoService.executeDemoCall("fail")).thenReturn(fallbackResponse);

        assertEquals(HttpStatus.OK, controller.testCircuitBreaker("success").getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, controller.testCircuitBreaker("fail").getStatusCode());
    }

    @Test
    @DisplayName("getStatus - Retorna HTTP 200 OK")
    void testGetStatus() {
        CircuitBreakerStatusDTO statusDTO = CircuitBreakerStatusDTO.builder().state("CLOSED").build();
        when(circuitBreakerDemoService.getCircuitBreakerStatus()).thenReturn(statusDTO);

        assertEquals(HttpStatus.OK, controller.getStatus().getStatusCode());
    }
}
