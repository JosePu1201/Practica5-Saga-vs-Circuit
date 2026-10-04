package com.Inventory_Service.demo.service;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Inventory_Service.demo.dto.InventoryResponseDTO;
import com.Inventory_Service.demo.dto.ReleaseReserveRequestDTO;
import com.Inventory_Service.demo.dto.ReserveRequestDTO;
import com.Inventory_Service.demo.dto.ReserveResponseDTO;
import com.Inventory_Service.demo.dto.StockUpdateRequestDTO;
import com.Inventory_Service.demo.enums.ReserveState;
import com.Inventory_Service.demo.exception.InsufficientInventoryException;
import com.Inventory_Service.demo.exception.InvalidReservationStateException;
import com.Inventory_Service.demo.exception.ProductNotFoundException;
import com.Inventory_Service.demo.exception.ReservationNotFoundException;
import com.Inventory_Service.demo.model.InventoryModel;
import com.Inventory_Service.demo.model.ReserveInventory;
import com.Inventory_Service.demo.repository.InventoryRepository;
import com.Inventory_Service.demo.repository.ReserveInventoryRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ReserveInventoryRepository reserveInventoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private InventoryModel mockInventory;
    private ReserveInventory mockReserve;

    @BeforeEach
    void setUp() {
        mockInventory = new InventoryModel();
        mockInventory.setProductId(1L);
        mockInventory.setQuantity(100);
        mockInventory.setReservedStock(10);

        mockReserve = new ReserveInventory();
        mockReserve.setReserveId(10L);
        mockReserve.setInventoryModel(mockInventory);
        mockReserve.setQuantity(5);
        mockReserve.setStatus(ReserveState.PENDING);
    }

    @Test
    @DisplayName("getInventoryByProductId - Retorna inventario cuando existe")
    void testGetInventoryByProductIdSuccess() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));

        InventoryResponseDTO response = inventoryService.getInventoryByProductId(1L);

        assertNotNull(response);
        assertEquals(1L, response.getProductId());
    }

    @Test
    @DisplayName("getInventoryByProductId - Lanza ProductNotFoundException cuando no existe")
    void testGetInventoryByProductIdNotFound() {
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> inventoryService.getInventoryByProductId(99L));
    }

    @Test
    @DisplayName("getAllInventories - Retorna todos los productos")
    void testGetAllInventories() {
        when(inventoryRepository.findAll()).thenReturn(List.of(mockInventory));

        List<InventoryResponseDTO> list = inventoryService.getAllInventories();

        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("reserveStock - Reserva exitosamente cuando hay stock suficiente")
    void testReserveStockSuccess() {
        ReserveRequestDTO request = new ReserveRequestDTO();
        request.setProductId(1L);
        request.setQuantity(20);

        when(inventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockInventory));
        when(reserveInventoryRepository.save(any(ReserveInventory.class))).thenReturn(mockReserve);

        ReserveResponseDTO response = inventoryService.reserveStock(request);

        assertNotNull(response);
        verify(inventoryRepository).save(mockInventory);
    }

    @Test
    @DisplayName("reserveStock - Lanza InsufficientInventoryException si excede stock disponible")
    void testReserveStockInsufficientStock() {
        ReserveRequestDTO request = new ReserveRequestDTO();
        request.setProductId(1L);
        request.setQuantity(200);

        when(inventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockInventory));

        assertThrows(InsufficientInventoryException.class, () -> inventoryService.reserveStock(request));
    }

    @Test
    @DisplayName("releaseReservation - Libera stock reservado exitosamente")
    void testReleaseReservationSuccess() {
        ReleaseReserveRequestDTO request = new ReleaseReserveRequestDTO();
        request.setReserveId(10L);

        when(reserveInventoryRepository.findById(10L)).thenReturn(Optional.of(mockReserve));
        when(inventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockInventory));
        when(reserveInventoryRepository.save(any(ReserveInventory.class))).thenReturn(mockReserve);

        ReserveResponseDTO response = inventoryService.releaseReservation(request);

        assertNotNull(response);
        assertEquals(ReserveState.REJECTED, mockReserve.getStatus());
        verify(inventoryRepository).save(mockInventory);
    }

    @Test
    @DisplayName("releaseReservation - Lanza InvalidReservationStateException si la reserva ya estaba REJECTED")
    void testReleaseReservationAlreadyRejected() {
        mockReserve.setStatus(ReserveState.REJECTED);
        ReleaseReserveRequestDTO request = new ReleaseReserveRequestDTO();
        request.setReserveId(10L);

        when(reserveInventoryRepository.findById(10L)).thenReturn(Optional.of(mockReserve));

        assertThrows(InvalidReservationStateException.class, () -> inventoryService.releaseReservation(request));
    }

    @Test
    @DisplayName("releaseReservation - Lanza ReservationNotFoundException si no existe reserva")
    void testReleaseReservationNotFound() {
        ReleaseReserveRequestDTO request = new ReleaseReserveRequestDTO();
        request.setReserveId(999L);

        when(reserveInventoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> inventoryService.releaseReservation(request));
    }

    @Test
    @DisplayName("saveOrUpdateStock - Actualiza stock exitosamente")
    void testSaveOrUpdateStockSuccess() {
        StockUpdateRequestDTO request = new StockUpdateRequestDTO();
        request.setQuantity(50);

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));
        when(inventoryRepository.save(any(InventoryModel.class))).thenReturn(mockInventory);

        InventoryResponseDTO response = inventoryService.saveOrUpdateStock(1L, request);

        assertNotNull(response);
        assertEquals(50, mockInventory.getQuantity());
    }

    @Test
    @DisplayName("saveOrUpdateStock - Lanza InvalidReservationStateException si nuevo stock < reserved")
    void testSaveOrUpdateStockInvalid() {
        StockUpdateRequestDTO request = new StockUpdateRequestDTO();
        request.setQuantity(5); // reservedStock es 10

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));

        assertThrows(InvalidReservationStateException.class, () -> inventoryService.saveOrUpdateStock(1L, request));
    }

    @Test
    @DisplayName("getReservationById, getAllReservations, getReservationsByProductId, getAvailableStock")
    void testQueryMethods() {
        when(reserveInventoryRepository.findById(10L)).thenReturn(Optional.of(mockReserve));
        when(reserveInventoryRepository.findAll()).thenReturn(List.of(mockReserve));
        when(reserveInventoryRepository.findByInventoryModelProductId(1L)).thenReturn(List.of(mockReserve));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));

        assertNotNull(inventoryService.getReservationById(10L));
        assertEquals(1, inventoryService.getAllReservations().size());
        assertEquals(1, inventoryService.getReservationsByProductId(1L).size());
        assertEquals(90, inventoryService.getAvailableStock(1L));
    }

    @Test
    @DisplayName("saveOrUpdateStock - productId null crea nuevo inventario")
    void testSaveOrUpdateStockNullProductId() {
        StockUpdateRequestDTO request = new StockUpdateRequestDTO();
        request.setQuantity(50);

        when(inventoryRepository.save(any(InventoryModel.class))).thenAnswer(i -> i.getArgument(0));

        InventoryResponseDTO response = inventoryService.saveOrUpdateStock(null, request);

        assertNotNull(response);
        assertEquals(50, response.getQuantity());
    }

    @Test
    @DisplayName("releaseReservationById - Cancela reserva por id")
    void testReleaseReservationById() {
        when(reserveInventoryRepository.findById(10L)).thenReturn(Optional.of(mockReserve));
        when(inventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockInventory));
        when(reserveInventoryRepository.save(any(ReserveInventory.class))).thenReturn(mockReserve);

        ReserveResponseDTO response = inventoryService.releaseReservationById(10L);

        assertNotNull(response);
        assertEquals(ReserveState.REJECTED, mockReserve.getStatus());
    }
}
