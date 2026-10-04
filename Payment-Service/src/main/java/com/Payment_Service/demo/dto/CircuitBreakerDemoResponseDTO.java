package com.Payment_Service.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CircuitBreakerDemoResponseDTO {
    private String circuitBreakerName;
    private String circuitState;
    private String mode;
    private boolean fallbackTriggered;
    private String message;
    private String reason;
    private String remoteResponse;
}