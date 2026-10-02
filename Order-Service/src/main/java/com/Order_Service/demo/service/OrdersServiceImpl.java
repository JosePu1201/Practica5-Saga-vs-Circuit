package com.Order_Service.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Order_Service.demo.dto.OrderRequestDTO;
import com.Order_Service.demo.dto.OrderResponseDTO;
import com.Order_Service.demo.dto.OrderStatusUpdateDTO;
import com.Order_Service.demo.enums.StatusOrder;
import com.Order_Service.demo.exception.OrderNotFoundException;
import com.Order_Service.demo.exception.OrderStateConflictException;
import com.Order_Service.demo.model.OrdersModel;
import com.Order_Service.demo.repository.OrdersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrdersServiceImpl implements OrdersService {

    private final OrdersRepository ordersRepository;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO requestDto) {
        LocalDateTime now = LocalDateTime.now();

        OrdersModel newOrder = new OrdersModel();
        newOrder.setCustomerId(requestDto.getCustomerId());
        newOrder.setTotal(requestDto.getTotal());
        newOrder.setStatus(StatusOrder.PENDING);
        newOrder.setCreatedAt(now);
        newOrder.setUpdatedAt(now);

        OrdersModel saved = ordersRepository.save(newOrder);
        return OrderResponseDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        OrdersModel order = ordersRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return OrderResponseDTO.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return ordersRepository.findAll().stream()
                .map(OrderResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long id, OrderStatusUpdateDTO updateDto) {
        OrdersModel order = ordersRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        StatusOrder targetStatus = updateDto.getStatus();

        if (order.getStatus() == StatusOrder.CANCELLED) {
            throw new OrderStateConflictException("No se puede modificar el estado de un pedido que ya está CANCELLED");
        }

        if (order.getStatus() == StatusOrder.COMPLETED && targetStatus == StatusOrder.PENDING) {
            throw new OrderStateConflictException("No se permite revertir un pedido de COMPLETED a PENDING");
        }

        order.setStatus(targetStatus);
        order.setUpdatedAt(LocalDateTime.now());

        OrdersModel updated = ordersRepository.save(order);
        return OrderResponseDTO.fromEntity(updated);
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelOrder(Long id) {
        OrdersModel order = ordersRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() == StatusOrder.CANCELLED) {
            throw new OrderStateConflictException("El pedido con ID " + id + " ya se encuentra cancelado");
        }

        // Cancelación lógica para transacciones compensatorias en Saga
        order.setStatus(StatusOrder.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        OrdersModel cancelled = ordersRepository.save(order);
        return OrderResponseDTO.fromEntity(cancelled);
    }
}