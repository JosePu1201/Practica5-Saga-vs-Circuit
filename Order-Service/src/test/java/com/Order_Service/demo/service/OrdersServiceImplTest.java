package com.Order_Service.demo.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Order_Service.demo.dto.OrderRequestDTO;
import com.Order_Service.demo.dto.OrderResponseDTO;
import com.Order_Service.demo.dto.OrderStatusUpdateDTO;
import com.Order_Service.demo.enums.StatusOrder;
import com.Order_Service.demo.event.OrderCreatedPayload;
import com.Order_Service.demo.exception.OrderNotFoundException;
import com.Order_Service.demo.exception.OrderStateConflictException;
import com.Order_Service.demo.model.OrdersModel;
import com.Order_Service.demo.publisher.OrderEventPublisher;
import com.Order_Service.demo.repository.OrdersRepository;

@ExtendWith(MockitoExtension.class)
class OrdersServiceImplTest {

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrdersServiceImpl ordersService;

    private OrdersModel mockOrder;

    @BeforeEach
    void setUp() {
        mockOrder = new OrdersModel();
        mockOrder.setId(1L);
        mockOrder.setCustomerId(100L);
        mockOrder.setTotal(250);
        mockOrder.setStatus(StatusOrder.PENDING);
    }

    @Test
    @DisplayName("createOrder - Debe guardar la orden en PENDING y publicar el evento order.created")
    void testCreateOrderSuccess() {
        OrderRequestDTO request = OrderRequestDTO.builder()
                .customerId(100L)
                .total(250)
                .productId(5L)
                .quantity(2)
                .address("Calle 123")
                .build();

        when(ordersRepository.save(any(OrdersModel.class))).thenReturn(mockOrder);

        OrderResponseDTO response = ordersService.createOrder(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(StatusOrder.PENDING, response.getStatus());

        verify(ordersRepository).save(any(OrdersModel.class));
        verify(orderEventPublisher).publishOrderCreated(any(OrderCreatedPayload.class));
    }

    @Test
    @DisplayName("getOrderById - Debe retornar la orden cuando existe")
    void testGetOrderByIdSuccess() {
        when(ordersRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        OrderResponseDTO response = ordersService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("getOrderById - Debe lanzar OrderNotFoundException cuando la orden no existe")
    void testGetOrderByIdNotFound() {
        when(ordersRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> ordersService.getOrderById(999L));
    }

    @Test
    @DisplayName("getAllOrders - Debe retornar la lista completa de órdenes")
    void testGetAllOrders() {
        when(ordersRepository.findAll()).thenReturn(List.of(mockOrder));

        List<OrderResponseDTO> result = ordersService.getAllOrders();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("updateOrderStatus - Debe actualizar estado exitosamente")
    void testUpdateOrderStatusSuccess() {
        OrderStatusUpdateDTO updateDto = new OrderStatusUpdateDTO();
        updateDto.setStatus(StatusOrder.COMPLETED);

        when(ordersRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(ordersRepository.save(any(OrdersModel.class))).thenReturn(mockOrder);

        OrderResponseDTO result = ordersService.updateOrderStatus(1L, updateDto);

        assertNotNull(result);
        verify(ordersRepository).save(mockOrder);
    }

    @Test
    @DisplayName("updateOrderStatus - Lanzar excepción si la orden ya está CANCELLED")
    void testUpdateOrderStatusCancelledConflict() {
        mockOrder.setStatus(StatusOrder.CANCELLED);
        OrderStatusUpdateDTO updateDto = new OrderStatusUpdateDTO();
        updateDto.setStatus(StatusOrder.COMPLETED);

        when(ordersRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(OrderStateConflictException.class, () -> ordersService.updateOrderStatus(1L, updateDto));
    }

    @Test
    @DisplayName("updateOrderStatus - Lanzar excepción si intenta cambiar COMPLETED a PENDING")
    void testUpdateOrderStatusCompletedToPendingConflict() {
        mockOrder.setStatus(StatusOrder.COMPLETED);
        OrderStatusUpdateDTO updateDto = new OrderStatusUpdateDTO();
        updateDto.setStatus(StatusOrder.PENDING);

        when(ordersRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(OrderStateConflictException.class, () -> ordersService.updateOrderStatus(1L, updateDto));
    }

    @Test
    @DisplayName("cancelOrder - Debe cancelar la orden exitosamente")
    void testCancelOrderSuccess() {
        when(ordersRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(ordersRepository.save(any(OrdersModel.class))).thenReturn(mockOrder);

        OrderResponseDTO result = ordersService.cancelOrder(1L);

        assertNotNull(result);
        verify(ordersRepository).save(mockOrder);
    }

    @Test
    @DisplayName("cancelOrder - Lanzar excepción si ya estaba CANCELLED")
    void testCancelOrderAlreadyCancelled() {
        mockOrder.setStatus(StatusOrder.CANCELLED);
        when(ordersRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(OrderStateConflictException.class, () -> ordersService.cancelOrder(1L));
    }
}
