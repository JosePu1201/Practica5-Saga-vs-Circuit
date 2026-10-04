package com.Inventory_Service.demo.dto;

import java.time.LocalDateTime;

import com.Inventory_Service.demo.model.InventoryModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponseDTO {

    private Long productId;
    private Integer quantity;
    private Integer reservedStock;
    private Integer availableStock;
    private LocalDateTime dateUpdated;

    public static InventoryResponseDTO fromEntity(InventoryModel entity) {
        if (entity == null) {
            return null;
        }
        int total = entity.getQuantity() != null ? entity.getQuantity() : 0;
        int reserved = entity.getReservedStock() != null ? entity.getReservedStock() : 0;
        int available = Math.max(0, total - reserved);

        return InventoryResponseDTO.builder()
                .productId(entity.getProductId())
                .quantity(total)
                .reservedStock(reserved)
                .availableStock(available)
                .dateUpdated(entity.getDateUpdated())
                .build();
    }
}