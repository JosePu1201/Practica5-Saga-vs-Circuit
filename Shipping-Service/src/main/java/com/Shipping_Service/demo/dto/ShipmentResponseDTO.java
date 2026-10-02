package com.Shipping_Service.demo.dto;

import java.time.LocalDateTime;

import com.Shipping_Service.demo.enums.ShipmentStatus;
import com.Shipping_Service.demo.model.ShipmentsModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponseDTO {

    private Long shipmentId;
    private Long orderId;
    private String address;
    private ShipmentStatus status;
    private LocalDateTime scheduleDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String trackingSimulationNote;

    public static ShipmentResponseDTO fromEntity(ShipmentsModel entity, String simulationNote) {
        if (entity == null) {
            return null;
        }
        return ShipmentResponseDTO.builder()
                .shipmentId(entity.getShipmentId())
                .orderId(entity.getOrderId())
                .address(entity.getAddress())
                .status(entity.getStatus())
                .scheduleDate(entity.getScheduleDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .trackingSimulationNote(simulationNote)
                .build();
    }
}