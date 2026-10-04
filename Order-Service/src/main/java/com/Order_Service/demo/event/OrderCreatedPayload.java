package com.Order_Service.demo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedPayload {
    private Long orderId;
    private Long customerId;
    private Integer total; // Monto consumido por Payment Service
    private Long productId; // Identificador de producto para Inventory Service
    private Integer quantity; // Cantidad física a reservar
    private String address; // Dirección para Shipping Service
}