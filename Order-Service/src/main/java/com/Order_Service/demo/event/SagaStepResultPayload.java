package com.Order_Service.demo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaStepResultPayload {
    private Long orderId;
    private Long paymentId;
    private Long reserveId;
    private Long shipmentId;
    private String status;
    private String reason;
    private String details;
}