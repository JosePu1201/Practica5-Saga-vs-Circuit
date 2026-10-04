package com.Payment_Service.demo.dto;

import java.time.LocalDateTime;

import com.Payment_Service.demo.enums.StatusPayment;
import com.Payment_Service.demo.model.PaymentsModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {

    private Long id;
    private Long orderId;
    private Integer amount;
    private StatusPayment status;
    private String simulatedMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentResponseDTO fromEntity(PaymentsModel entity, String message) {
        if (entity == null) {
            return null;
        }
        return PaymentResponseDTO.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .simulatedMessage(message)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}