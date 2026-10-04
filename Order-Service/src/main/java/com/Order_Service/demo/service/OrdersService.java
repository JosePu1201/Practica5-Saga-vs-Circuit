package com.Order_Service.demo.service;

import java.util.List;
import com.Order_Service.demo.dto.OrderRequestDTO;
import com.Order_Service.demo.dto.OrderResponseDTO;
import com.Order_Service.demo.dto.OrderStatusUpdateDTO;

public interface OrdersService {
    OrderResponseDTO createOrder(OrderRequestDTO requestDto);

    OrderResponseDTO getOrderById(Long id);

    List<OrderResponseDTO> getAllOrders();

    OrderResponseDTO updateOrderStatus(Long id, OrderStatusUpdateDTO updateDto);

    OrderResponseDTO cancelOrder(Long id);
}