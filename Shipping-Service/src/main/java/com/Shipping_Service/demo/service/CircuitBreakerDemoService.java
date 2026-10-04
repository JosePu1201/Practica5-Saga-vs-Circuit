package com.Shipping_Service.demo.service;

import com.Shipping_Service.demo.dto.CircuitBreakerDemoResponseDTO;
import com.Shipping_Service.demo.dto.CircuitBreakerStatusDTO;

public interface CircuitBreakerDemoService {
    CircuitBreakerDemoResponseDTO executeDemoCall(String mode);

    CircuitBreakerStatusDTO getCircuitBreakerStatus();
}