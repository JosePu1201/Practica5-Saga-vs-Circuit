package com.Shipping_Service.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerStatusDTO {
    private String circuitBreakerName;
    private String state;
    private float failureRate;
    private float slowCallRate;
    private int numberOfBufferedCalls;
    private int numberOfFailedCalls;
    private int numberOfSuccessfulCalls;
    private long numberOfNotPermittedCalls;
    private int slidingWindowSize;
    private int minimumNumberOfCalls;
    private float failureRateThreshold;
    private long waitDurationInOpenStateSeconds;
    private int permittedNumberOfCallsInHalfOpenState;
}