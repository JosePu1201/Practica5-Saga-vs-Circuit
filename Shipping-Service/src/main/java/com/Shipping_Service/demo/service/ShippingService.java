package com.Shipping_Service.demo.service;

import java.util.List;

import com.Shipping_Service.demo.dto.ShipmentResponseDTO;
import com.Shipping_Service.demo.dto.ShipmentScheduleRequestDTO;
import com.Shipping_Service.demo.dto.ShipmentStatusResponseDTO;

public interface ShippingService {
    ShipmentResponseDTO scheduleShipment(ShipmentScheduleRequestDTO requestDto);

    ShipmentResponseDTO getShipmentById(Long shipmentId);

    List<ShipmentResponseDTO> getShipmentsByOrderId(Long orderId);

    List<ShipmentResponseDTO> getAllShipments();

    ShipmentResponseDTO cancelShipment(Long shipmentId);

    ShipmentStatusResponseDTO getShipmentStatus(Long shipmentId);
}