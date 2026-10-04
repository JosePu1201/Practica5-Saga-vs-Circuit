package com.Shipping_Service.demo.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Shipping_Service.demo.dto.CircuitBreakerDemoResponseDTO;
import com.Shipping_Service.demo.service.CircuitBreakerDemoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/circuit-breaker")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "circuitbreaker.demo.enabled", havingValue = "true")
public class CircuitBreakerDemoController {

    private final CircuitBreakerDemoService circuitBreakerDemoService;

    @GetMapping("/test")
    public ResponseEntity testCircuitBreaker(@RequestParam(required = false) String mode) {
        CircuitBreakerDemoResponseDTO response = circuitBreakerDemoService.executeDemoCall(mode);
        if (response.isFallbackTriggered()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity getStatus() {
        return ResponseEntity.ok(circuitBreakerDemoService.getCircuitBreakerStatus());
    }
}