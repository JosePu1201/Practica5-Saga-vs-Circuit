package com.Order_Service.demo.service;

import com.Order_Service.demo.dto.CircuitBreakerDemoResponseDTO;
import com.Order_Service.demo.dto.CircuitBreakerStatusDTO;

public interface CircuitBreakerDemoService {
    CircuitBreakerDemoResponseDTO executeDemoCall(String mode);

    CircuitBreakerStatusDTO getCircuitBreakerStatus();
}