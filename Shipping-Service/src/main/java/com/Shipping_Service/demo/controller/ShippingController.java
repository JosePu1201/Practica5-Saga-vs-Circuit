package com.Shipping_Service.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Shipping_Service.demo.dto.ShipmentResponseDTO;
import com.Shipping_Service.demo.dto.ShipmentScheduleRequestDTO;
import com.Shipping_Service.demo.dto.ShipmentStatusResponseDTO;
import com.Shipping_Service.demo.service.ShippingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/schedule")
    public ResponseEntity<ShipmentResponseDTO> scheduleShipment(
            @Valid @RequestBody ShipmentScheduleRequestDTO requestDto) {
        ShipmentResponseDTO created = shippingService.scheduleShipment(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping
    public ResponseEntity<ShipmentResponseDTO> scheduleShipmentAlias(
            @Valid @RequestBody ShipmentScheduleRequestDTO requestDto) {
        ShipmentResponseDTO created = shippingService.scheduleShipment(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponseDTO> getShipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(shippingService.getShipmentById(id));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ShipmentStatusResponseDTO> getShipmentStatus(@PathVariable Long id) {
        return ResponseEntity.ok(shippingService.getShipmentStatus(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<ShipmentResponseDTO>> getShipmentsByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(shippingService.getShipmentsByOrderId(orderId));
    }

    @GetMapping
    public ResponseEntity<List<ShipmentResponseDTO>> getAllShipments() {
        return ResponseEntity.ok(shippingService.getAllShipments());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ShipmentResponseDTO> cancelShipmentDelete(@PathVariable Long id) {
        return ResponseEntity.ok(shippingService.cancelShipment(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ShipmentResponseDTO> cancelShipmentPut(@PathVariable Long id) {
        return ResponseEntity.ok(shippingService.cancelShipment(id));
    }
}