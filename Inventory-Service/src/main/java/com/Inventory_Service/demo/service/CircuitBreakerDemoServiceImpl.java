package com.Inventory_Service.demo.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import com.Inventory_Service.demo.client.ShippingDemoClient;
import com.Inventory_Service.demo.dto.CircuitBreakerDemoResponseDTO;
import com.Inventory_Service.demo.dto.CircuitBreakerStatusDTO;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CircuitBreakerDemoServiceImpl implements CircuitBreakerDemoService {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerDemoServiceImpl.class);

    private final CircuitBreaker inventoryDemoCircuitBreaker;
    private final ShippingDemoClient shippingDemoClient;

    @Override
    public CircuitBreakerDemoResponseDTO executeDemoCall(String mode) {
        if (mode == null || mode.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El parámetro 'mode' es obligatorio. Valores válidos: success, fail, slow");
        }

        String normalizedMode = mode.trim().toLowerCase();
        if (!List.of("success", "fail", "slow").contains(normalizedMode)) {
            throw new IllegalArgumentException(
                    "Modo inválido: '" + mode + "'. Valores permitidos: success, fail, slow");
        }

        try {
            String responseBody = inventoryDemoCircuitBreaker.executeSupplier(
                    () -> shippingDemoClient.callDemoDependency(normalizedMode));

            return CircuitBreakerDemoResponseDTO.builder()
                    .circuitBreakerName(inventoryDemoCircuitBreaker.getName())
                    .circuitState(inventoryDemoCircuitBreaker.getState().name())
                    .mode(normalizedMode)
                    .fallbackTriggered(false)
                    .message("Llamada síncrona ejecutada exitosamente a través del Circuit Breaker")
                    .remoteResponse(responseBody)
                    .build();

        } catch (CallNotPermittedException ex) {
            log.warn("[Circuit Breaker: OPEN] Llamada rechazada preventivamente. Razón: {}", ex.getMessage());
            return buildFallbackResponse(normalizedMode,
                    "Circuito en estado OPEN: llamada rechazada preventivamente por Resilience4j (CallNotPermittedException)");

        } catch (RestClientResponseException ex) {
            log.error("[Circuit Breaker: HTTP ERROR] La dependencia remota respondió status {}: {}",
                    ex.getStatusCode(), ex.getMessage());
            return buildFallbackResponse(normalizedMode,
                    "Error devuelto por dependencia remota: HTTP " + ex.getStatusCode().value() + " - "
                            + ex.getStatusText());

        } catch (ResourceAccessException ex) {
            log.error("[Circuit Breaker: TIMEOUT/RED] Error de conexión o timeout: {}", ex.getMessage());
            return buildFallbackResponse(normalizedMode,
                    "Timeout de lectura o fallo de conexión hacia la dependencia remota (" + ex.getMessage() + ")");

        } catch (Exception ex) {
            log.error("[Circuit Breaker: ERROR] Fallo no controlado: {}", ex.getMessage());
            return buildFallbackResponse(normalizedMode,
                    "Fallo inesperado al conectar con dependencia remota: " + ex.getClass().getSimpleName());
        }
    }

    private CircuitBreakerDemoResponseDTO buildFallbackResponse(String mode, String reason) {
        return CircuitBreakerDemoResponseDTO.builder()
                .circuitBreakerName(inventoryDemoCircuitBreaker.getName())
                .circuitState(inventoryDemoCircuitBreaker.getState().name())
                .mode(mode)
                .fallbackTriggered(true)
                .message("Servicio no disponible: Se activó el mecanismo de contingencia (Fallback)")
                .reason(reason)
                .build();
    }

    @Override
    public CircuitBreakerStatusDTO getCircuitBreakerStatus() {
        CircuitBreaker.Metrics metrics = inventoryDemoCircuitBreaker.getMetrics();
        return CircuitBreakerStatusDTO.builder()
                .circuitBreakerName(inventoryDemoCircuitBreaker.getName())
                .state(inventoryDemoCircuitBreaker.getState().name())
                .failureRate(metrics.getFailureRate())
                .slowCallRate(metrics.getSlowCallRate())
                .numberOfBufferedCalls(metrics.getNumberOfBufferedCalls())
                .numberOfFailedCalls(metrics.getNumberOfFailedCalls())
                .numberOfSuccessfulCalls(metrics.getNumberOfSuccessfulCalls())
                .numberOfNotPermittedCalls(metrics.getNumberOfNotPermittedCalls())
                .slidingWindowSize(inventoryDemoCircuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize())
                .minimumNumberOfCalls(inventoryDemoCircuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls())
                .failureRateThreshold(inventoryDemoCircuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
                .waitDurationInOpenStateSeconds(10L)
                .permittedNumberOfCallsInHalfOpenState(
                        inventoryDemoCircuitBreaker.getCircuitBreakerConfig()
                                .getPermittedNumberOfCallsInHalfOpenState())
                .build();
    }
}