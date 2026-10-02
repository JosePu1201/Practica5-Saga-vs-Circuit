package com.Inventory_Service.demo.service;

import java.util.List;

import com.Inventory_Service.demo.dto.InventoryResponseDTO;
import com.Inventory_Service.demo.dto.ReleaseReserveRequestDTO;
import com.Inventory_Service.demo.dto.ReserveRequestDTO;
import com.Inventory_Service.demo.dto.ReserveResponseDTO;
import com.Inventory_Service.demo.dto.StockUpdateRequestDTO;

public interface InventoryService {
    InventoryResponseDTO getInventoryByProductId(Long productId);

    List<InventoryResponseDTO> getAllInventories();

    InventoryResponseDTO saveOrUpdateStock(Long productId, StockUpdateRequestDTO requestDto);

    ReserveResponseDTO reserveStock(ReserveRequestDTO requestDto);

    ReserveResponseDTO getReservationById(Long reserveId);

    List<ReserveResponseDTO> getAllReservations();

    List<ReserveResponseDTO> getReservationsByProductId(Long productId);

    ReserveResponseDTO releaseReservation(ReleaseReserveRequestDTO requestDto);

    ReserveResponseDTO releaseReservationById(Long reserveId);

    Integer getAvailableStock(Long productId);
}