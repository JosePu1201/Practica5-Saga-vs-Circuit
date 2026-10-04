package com.Shipping_Service.demo.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.Shipping_Service.demo.dto.ShipmentResponseDTO;
import com.Shipping_Service.demo.dto.ShipmentScheduleRequestDTO;
import com.Shipping_Service.demo.dto.ShipmentStatusResponseDTO;
import com.Shipping_Service.demo.enums.ShipmentStatus;
import com.Shipping_Service.demo.service.ShippingService;

@ExtendWith(MockitoExtension.class)
class ShippingControllerTest {

    @Mock
    private ShippingService shippingService;

    @InjectMocks
    private ShippingController shippingController;

    private ShipmentResponseDTO mockShipmentDTO;

    @BeforeEach
    void setUp() {
        mockShipmentDTO = new ShipmentResponseDTO();
        mockShipmentDTO.setShipmentId(1L);
        mockShipmentDTO.setStatus(ShipmentStatus.PENDING);
    }

    @Test
    @DisplayName("scheduleShipment - Retorna HTTP 201 CREATED")
    void testScheduleShipment() {
        when(shippingService.scheduleShipment(any())).thenReturn(mockShipmentDTO);
        ResponseEntity<ShipmentResponseDTO> response = shippingController.scheduleShipment(new ShipmentScheduleRequestDTO());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("getShipmentById, getShipmentsByOrderId, getAllShipments, getShipmentStatus - Retorna HTTP 200 OK")
    void testGetQueries() {
        when(shippingService.getShipmentById(1L)).thenReturn(mockShipmentDTO);
        when(shippingService.getShipmentsByOrderId(10L)).thenReturn(List.of(mockShipmentDTO));
        when(shippingService.getAllShipments()).thenReturn(List.of(mockShipmentDTO));
        when(shippingService.getShipmentStatus(1L)).thenReturn(ShipmentStatusResponseDTO.builder().build());

        assertEquals(HttpStatus.OK, shippingController.getShipmentById(1L).getStatusCode());
        assertEquals(HttpStatus.OK, shippingController.getShipmentsByOrderId(10L).getStatusCode());
        assertEquals(HttpStatus.OK, shippingController.getAllShipments().getStatusCode());
        assertEquals(HttpStatus.OK, shippingController.getShipmentStatus(1L).getStatusCode());
    }

    @Test
    @DisplayName("cancelShipment - Retorna HTTP 200 OK")
    void testCancelShipment() {
        when(shippingService.cancelShipment(1L)).thenReturn(mockShipmentDTO);
        assertEquals(HttpStatus.OK, shippingController.cancelShipmentDelete(1L).getStatusCode());
        assertEquals(HttpStatus.OK, shippingController.cancelShipmentPut(1L).getStatusCode());
    }
}
