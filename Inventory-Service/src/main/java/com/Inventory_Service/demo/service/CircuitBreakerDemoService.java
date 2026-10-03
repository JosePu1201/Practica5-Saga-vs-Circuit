package com.Inventory_Service.demo.service;

import com.Inventory_Service.demo.dto.CircuitBreakerDemoResponseDTO;
import com.Inventory_Service.demo.dto.CircuitBreakerStatusDTO;

public interface CircuitBreakerDemoService {
    CircuitBreakerDemoResponseDTO executeDemoCall(String mode);

    CircuitBreakerStatusDTO getCircuitBreakerStatus();
}