package com.Order_Service.demo.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.Order_Service.demo.dto.OrderRequestDTO;
import com.Order_Service.demo.dto.OrderResponseDTO;
import com.Order_Service.demo.dto.OrderStatusUpdateDTO;
import com.Order_Service.demo.enums.StatusOrder;
import com.Order_Service.demo.service.OrdersService;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrdersService ordersService;

    @InjectMocks
    private OrderController orderController;

    private OrderResponseDTO mockResponseDto;

    @BeforeEach
    void setUp() {
        mockResponseDto = OrderResponseDTO.builder()
                .id(1L)
                .customerId(100L)
                .total(150)
                .status(StatusOrder.PENDING)
                .build();
    }

    @Test
    @DisplayName("createOrder - Retorna HTTP 201 CREATED")
    void testCreateOrder() {
        OrderRequestDTO request = OrderRequestDTO.builder()
                .customerId(100L)
                .total(150)
                .productId(1L)
                .quantity(1)
                .address("Guatemala")
                .build();

        when(ordersService.createOrder(any(OrderRequestDTO.class))).thenReturn(mockResponseDto);

        ResponseEntity<OrderResponseDTO> response = orderController.createOrder(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
        verify(ordersService).createOrder(any(OrderRequestDTO.class));
    }

    @Test
    @DisplayName("getOrderById - Retorna HTTP 200 OK")
    void testGetOrderById() {
        when(ordersService.getOrderById(1L)).thenReturn(mockResponseDto);

        ResponseEntity<OrderResponseDTO> response = orderController.getOrderById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
        verify(ordersService).getOrderById(1L);
    }

    @Test
    @DisplayName("getAllOrders - Retorna HTTP 200 OK con la lista de órdenes")
    void testGetAllOrders() {
        when(ordersService.getAllOrders()).thenReturn(List.of(mockResponseDto));

        ResponseEntity<List<OrderResponseDTO>> response = orderController.getAllOrders();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(ordersService).getAllOrders();
    }

    @Test
    @DisplayName("updateOrderStatus - Retorna HTTP 200 OK")
    void testUpdateOrderStatus() {
        OrderStatusUpdateDTO updateDto = new OrderStatusUpdateDTO();
        updateDto.setStatus(StatusOrder.COMPLETED);

        when(ordersService.updateOrderStatus(eq(1L), any(OrderStatusUpdateDTO.class))).thenReturn(mockResponseDto);

        ResponseEntity<OrderResponseDTO> response = orderController.updateOrderStatus(1L, updateDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ordersService).updateOrderStatus(eq(1L), any(OrderStatusUpdateDTO.class));
    }

    @Test
    @DisplayName("cancelOrderPut - Retorna HTTP 200 OK")
    void testCancelOrderPut() {
        when(ordersService.cancelOrder(1L)).thenReturn(mockResponseDto);

        ResponseEntity<OrderResponseDTO> response = orderController.cancelOrderPut(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ordersService).cancelOrder(1L);
    }
}
