package com.Inventory_Service.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Inventory_Service.demo.dto.InventoryResponseDTO;
import com.Inventory_Service.demo.dto.ReleaseReserveRequestDTO;
import com.Inventory_Service.demo.dto.ReserveRequestDTO;
import com.Inventory_Service.demo.dto.ReserveResponseDTO;
import com.Inventory_Service.demo.dto.StockUpdateRequestDTO;
import com.Inventory_Service.demo.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponseDTO> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponseDTO>> getAllInventories() {
        return ResponseEntity.ok(inventoryService.getAllInventories());
    }

    @PutMapping("/{productId}/stock")
    public ResponseEntity<InventoryResponseDTO> updateStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockUpdateRequestDTO requestDto) {
        return ResponseEntity.ok(inventoryService.saveOrUpdateStock(productId, requestDto));
    }

    @PostMapping("/stock")
    public ResponseEntity<InventoryResponseDTO> createStock(@Valid @RequestBody StockUpdateRequestDTO requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.saveOrUpdateStock(null, requestDto));
    }

    @GetMapping("/{productId}/available")
    public ResponseEntity<Integer> getAvailableStock(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getAvailableStock(productId));
    }

    @PostMapping("/reserve")
    public ResponseEntity<ReserveResponseDTO> reserveStock(@Valid @RequestBody ReserveRequestDTO requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.reserveStock(requestDto));
    }

    @PostMapping("/release")
    public ResponseEntity<ReserveResponseDTO> releaseReservation(
            @Valid @RequestBody ReleaseReserveRequestDTO requestDto) {
        return ResponseEntity.ok(inventoryService.releaseReservation(requestDto));
    }

    @PostMapping("/reserves/{reserveId}/release")
    public ResponseEntity<ReserveResponseDTO> releaseReservationById(@PathVariable Long reserveId) {
        return ResponseEntity.ok(inventoryService.releaseReservationById(reserveId));
    }

    @GetMapping("/reserves/{reserveId}")
    public ResponseEntity<ReserveResponseDTO> getReservationById(@PathVariable Long reserveId) {
        return ResponseEntity.ok(inventoryService.getReservationById(reserveId));
    }

    @GetMapping("/reserves")
    public ResponseEntity<List<ReserveResponseDTO>> getAllReservations() {
        return ResponseEntity.ok(inventoryService.getAllReservations());
    }

    @GetMapping("/{productId}/reserves")
    public ResponseEntity<List<ReserveResponseDTO>> getReservationsByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getReservationsByProductId(productId));
    }
}