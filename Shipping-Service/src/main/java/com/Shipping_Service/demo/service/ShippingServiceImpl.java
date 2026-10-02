package com.Shipping_Service.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Shipping_Service.demo.dto.ShipmentResponseDTO;
import com.Shipping_Service.demo.dto.ShipmentScheduleRequestDTO;
import com.Shipping_Service.demo.dto.ShipmentStatusResponseDTO;
import com.Shipping_Service.demo.enums.ShipmentStatus;
import com.Shipping_Service.demo.exception.InvalidShipmentStateException;
import com.Shipping_Service.demo.exception.ShipmentNotFoundException;
import com.Shipping_Service.demo.model.ShipmentsModel;
import com.Shipping_Service.demo.repository.ShipmentsRepository;
import com.Shipping_Service.demo.service.ShippingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private final ShipmentsRepository shipmentsRepository;

    @Override
    @Transactional
    public ShipmentResponseDTO scheduleShipment(ShipmentScheduleRequestDTO requestDto) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetScheduleDate = requestDto.getScheduleDate() != null
                ? requestDto.getScheduleDate()
                : now.plusDays(2); // Estimación simulada por defecto de entrega a 48 horas

        ShipmentsModel shipment = new ShipmentsModel();
        shipment.setOrderId(requestDto.getOrderId());
        shipment.setAddress(requestDto.getAddress().trim());
        shipment.setScheduleDate(targetScheduleDate);
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment.setCreatedAt(now);
        shipment.setUpdatedAt(now);

        ShipmentsModel saved = shipmentsRepository.save(shipment);
        return ShipmentResponseDTO.fromEntity(saved, "Envío programado en cola de despacho (simulación local).");
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponseDTO getShipmentById(Long shipmentId) {
        ShipmentsModel shipment = shipmentsRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
        return ShipmentResponseDTO.fromEntity(shipment, "Consulta de envío exitosa.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponseDTO> getShipmentsByOrderId(Long orderId) {
        return shipmentsRepository.findByOrderId(orderId).stream()
                .map(s -> ShipmentResponseDTO.fromEntity(s, "Envío asignado al pedido " + orderId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponseDTO> getAllShipments() {
        return shipmentsRepository.findAll().stream()
                .map(s -> ShipmentResponseDTO.fromEntity(s, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShipmentResponseDTO cancelShipment(Long shipmentId) {
        ShipmentsModel shipment = shipmentsRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new InvalidShipmentStateException("El envío con ID " + shipmentId + " ya se encuentra cancelado");
        }

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new InvalidShipmentStateException(
                    "No es posible cancelar un paquete que ya ha sido entregado (DELIVERED)");
        }

        // Compensación de Saga: actualización lógica sin borrado físico
        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipment.setUpdatedAt(LocalDateTime.now());

        ShipmentsModel cancelled = shipmentsRepository.save(shipment);
        return ShipmentResponseDTO.fromEntity(cancelled, "Envío cancelado con éxito (compensación Saga simulada).");
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentStatusResponseDTO getShipmentStatus(Long shipmentId) {
        ShipmentsModel shipment = shipmentsRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        return ShipmentStatusResponseDTO.builder()
                .shipmentId(shipment.getShipmentId())
                .orderId(shipment.getOrderId())
                .status(shipment.getStatus())
                .lastUpdated(shipment.getUpdatedAt())
                .build();
    }
}