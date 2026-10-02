package com.Order_Service.demo.dto;

import java.time.LocalDateTime;
import com.Order_Service.demo.enums.StatusOrder;
import com.Order_Service.demo.model.OrdersModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    private Long id;
    private Long customerId;
    private Integer total;
    private StatusOrder status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponseDTO fromEntity(OrdersModel entity) {
        if (entity == null) {
            return null;
        }
        return OrderResponseDTO.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .total(entity.getTotal())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}