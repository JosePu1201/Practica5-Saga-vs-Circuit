package com.Shipping_Service.demo.service;

import java.time.LocalDateTime;
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
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Shipping_Service.demo.dto.ShipmentResponseDTO;
import com.Shipping_Service.demo.dto.ShipmentScheduleRequestDTO;
import com.Shipping_Service.demo.dto.ShipmentStatusResponseDTO;
import com.Shipping_Service.demo.enums.ShipmentStatus;
import com.Shipping_Service.demo.exception.InvalidShipmentStateException;
import com.Shipping_Service.demo.exception.ShipmentNotFoundException;
import com.Shipping_Service.demo.model.ShipmentsModel;
import com.Shipping_Service.demo.repository.ShipmentsRepository;

@ExtendWith(MockitoExtension.class)
class ShippingServiceImplTest {

    @Mock
    private ShipmentsRepository shipmentsRepository;

    @InjectMocks
    private ShippingServiceImpl shippingService;

    private ShipmentsModel mockShipment;

    @BeforeEach
    void setUp() {
        mockShipment = new ShipmentsModel();
        mockShipment.setShipmentId(1L);
        mockShipment.setOrderId(10L);
        mockShipment.setAddress("Calle 123");
        mockShipment.setStatus(ShipmentStatus.PENDING);
        mockShipment.setScheduleDate(LocalDateTime.now().plusDays(2));
        mockShipment.setCreatedAt(LocalDateTime.now());
        mockShipment.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("scheduleShipment - Programa envío exitosamente con fecha específica o nula")
    void testScheduleShipment() {
        ShipmentScheduleRequestDTO request = new ShipmentScheduleRequestDTO();
        request.setOrderId(10L);
        request.setAddress(" Calle 123 ");

        when(shipmentsRepository.save(any(ShipmentsModel.class))).thenReturn(mockShipment);

        ShipmentResponseDTO response = shippingService.scheduleShipment(request);

        assertNotNull(response);
        assertEquals(1L, response.getShipmentId());
        assertEquals(ShipmentStatus.PENDING, response.getStatus());

        request.setScheduleDate(LocalDateTime.now().plusDays(3));
        ShipmentResponseDTO response2 = shippingService.scheduleShipment(request);
        assertNotNull(response2);
    }

    @Test
    @DisplayName("getShipmentById - Retorna envío si existe o lanza ShipmentNotFoundException")
    void testGetShipmentById() {
        when(shipmentsRepository.findById(1L)).thenReturn(Optional.of(mockShipment));
        when(shipmentsRepository.findById(99L)).thenReturn(Optional.empty());

        ShipmentResponseDTO response = shippingService.getShipmentById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getShipmentId());

        assertThrows(ShipmentNotFoundException.class, () -> shippingService.getShipmentById(99L));
    }

    @Test
    @DisplayName("getShipmentsByOrderId & getAllShipments & getShipmentStatus")
    void testQueries() {
        when(shipmentsRepository.findByOrderId(10L)).thenReturn(List.of(mockShipment));
        when(shipmentsRepository.findAll()).thenReturn(List.of(mockShipment));
        when(shipmentsRepository.findById(1L)).thenReturn(Optional.of(mockShipment));

        assertEquals(1, shippingService.getShipmentsByOrderId(10L).size());
        assertEquals(1, shippingService.getAllShipments().size());

        ShipmentStatusResponseDTO status = shippingService.getShipmentStatus(1L);
        assertNotNull(status);
        assertEquals(ShipmentStatus.PENDING, status.getStatus());
    }

    @Test
    @DisplayName("cancelShipment - Cancela envío o lanza InvalidShipmentStateException")
    void testCancelShipment() {
        when(shipmentsRepository.findById(1L)).thenReturn(Optional.of(mockShipment));
        when(shipmentsRepository.save(any(ShipmentsModel.class))).thenAnswer(i -> i.getArgument(0));

        ShipmentResponseDTO response = shippingService.cancelShipment(1L);
        assertEquals(ShipmentStatus.CANCELLED, response.getStatus());

        // Cancelar cancelado
        mockShipment.setStatus(ShipmentStatus.CANCELLED);
        assertThrows(InvalidShipmentStateException.class, () -> shippingService.cancelShipment(1L));

        // Cancelar entregado
        mockShipment.setStatus(ShipmentStatus.DELIVERED);
        assertThrows(InvalidShipmentStateException.class, () -> shippingService.cancelShipment(1L));
    }
}
