package com.Inventory_Service.demo.dto;

import java.time.LocalDateTime;

import com.Inventory_Service.demo.enums.ReserveState;
import com.Inventory_Service.demo.model.ReserveInventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveResponseDTO {

    private Long reserveId;
    private Long productId;
    private Integer quantity;
    private ReserveState status;
    private LocalDateTime dateReserved;

    public static ReserveResponseDTO fromEntity(ReserveInventory entity) {
        if (entity == null) {
            return null;
        }
        return ReserveResponseDTO.builder()
                .reserveId(entity.getReserveId())
                .productId(entity.getInventoryModel() != null ? entity.getInventoryModel().getProductId() : null)
                .quantity(entity.getQuantity())
                .status(entity.getStatus())
                .dateReserved(entity.getDateReserved())
                .build();
    }
}