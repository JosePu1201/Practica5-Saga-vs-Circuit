package com.Shipping_Service.demo.event;

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
    private Integer total;
    private Long productId;
    private Integer quantity;
    private String address;
}