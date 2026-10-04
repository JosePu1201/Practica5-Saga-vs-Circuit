package com.Inventory_Service.demo.controller;

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

import com.Inventory_Service.demo.dto.InventoryResponseDTO;
import com.Inventory_Service.demo.dto.ReleaseReserveRequestDTO;
import com.Inventory_Service.demo.dto.ReserveRequestDTO;
import com.Inventory_Service.demo.dto.ReserveResponseDTO;
import com.Inventory_Service.demo.dto.StockUpdateRequestDTO;
import com.Inventory_Service.demo.enums.ReserveState;
import com.Inventory_Service.demo.service.InventoryService;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    private InventoryResponseDTO mockInventoryDTO;
    private ReserveResponseDTO mockReserveDTO;

    @BeforeEach
    void setUp() {
        mockInventoryDTO = new InventoryResponseDTO();
        mockInventoryDTO.setProductId(1L);
        mockInventoryDTO.setQuantity(100);

        mockReserveDTO = new ReserveResponseDTO();
        mockReserveDTO.setReserveId(10L);
        mockReserveDTO.setStatus(ReserveState.PENDING);
    }

    @Test
    @DisplayName("getInventoryByProductId - Retorna HTTP 200 OK")
    void testGetInventoryByProductId() {
        when(inventoryService.getInventoryByProductId(1L)).thenReturn(mockInventoryDTO);

        ResponseEntity<InventoryResponseDTO> response = inventoryController.getInventoryByProductId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getProductId());
    }

    @Test
    @DisplayName("getAllInventories - Retorna HTTP 200 OK")
    void testGetAllInventories() {
        when(inventoryService.getAllInventories()).thenReturn(List.of(mockInventoryDTO));

        ResponseEntity<List<InventoryResponseDTO>> response = inventoryController.getAllInventories();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("reserveStock - Retorna HTTP 201 CREATED")
    void testReserveStock() {
        ReserveRequestDTO request = new ReserveRequestDTO();
        when(inventoryService.reserveStock(any())).thenReturn(mockReserveDTO);

        ResponseEntity<ReserveResponseDTO> response = inventoryController.reserveStock(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(inventoryService).reserveStock(any());
    }

    @Test
    @DisplayName("releaseReservation - Retorna HTTP 200 OK")
    void testReleaseReservation() {
        ReleaseReserveRequestDTO request = new ReleaseReserveRequestDTO();
        when(inventoryService.releaseReservation(any())).thenReturn(mockReserveDTO);

        ResponseEntity<ReserveResponseDTO> response = inventoryController.releaseReservation(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(inventoryService).releaseReservation(any());
    }

    @Test
    @DisplayName("saveOrUpdateStock - Retorna HTTP 200 OK")
    void testSaveOrUpdateStock() {
        StockUpdateRequestDTO request = new StockUpdateRequestDTO();
        when(inventoryService.saveOrUpdateStock(eq(1L), any())).thenReturn(mockInventoryDTO);

        ResponseEntity<InventoryResponseDTO> response = inventoryController.updateStock(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(inventoryService).saveOrUpdateStock(eq(1L), any());
    }
}
