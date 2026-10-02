package com.Shipping_Service.demo.dto;

import java.time.LocalDateTime;

import com.Shipping_Service.demo.enums.ShipmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusResponseDTO {

    private Long shipmentId;
    private Long orderId;
    private ShipmentStatus status;
    private LocalDateTime lastUpdated;
}