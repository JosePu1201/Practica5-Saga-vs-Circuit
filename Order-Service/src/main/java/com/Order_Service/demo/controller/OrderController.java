package com.Order_Service.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Order_Service.demo.dto.OrderRequestDTO;
import com.Order_Service.demo.dto.OrderResponseDTO;
import com.Order_Service.demo.dto.OrderStatusUpdateDTO;
import com.Order_Service.demo.service.OrdersService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrdersService ordersService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO requestDto) {
        OrderResponseDTO created = ordersService.createOrder(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ordersService.getOrderById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return ResponseEntity.ok(ordersService.getAllOrders());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateDTO updateDto) {
        return ResponseEntity.ok(ordersService.updateOrderStatus(id, updateDto));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrderPut(@PathVariable Long id) {
        return ResponseEntity.ok(ordersService.cancelOrder(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> cancelOrderDelete(@PathVariable Long id) {
        return ResponseEntity.ok(ordersService.cancelOrder(id));
    }
}