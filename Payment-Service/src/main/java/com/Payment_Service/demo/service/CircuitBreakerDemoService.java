package com.Payment_Service.demo.service;

import com.Payment_Service.demo.dto.CircuitBreakerDemoResponseDTO;
import com.Payment_Service.demo.dto.CircuitBreakerStatusDTO;

public interface CircuitBreakerDemoService {
    CircuitBreakerDemoResponseDTO executeDemoCall(String mode);

    CircuitBreakerStatusDTO getCircuitBreakerStatus();
}