package com.Inventory_Service.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ReserveInventoryRepository reserveInventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public InventoryResponseDTO getInventoryByProductId(Long productId) {
        InventoryModel inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return InventoryResponseDTO.fromEntity(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponseDTO> getAllInventories() {
        return inventoryRepository.findAll().stream()
                .map(InventoryResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryResponseDTO saveOrUpdateStock(Long productId, StockUpdateRequestDTO requestDto) {
        LocalDateTime now = LocalDateTime.now();
        InventoryModel inventory;

        if (productId != null) {
            inventory = inventoryRepository.findById(productId)
                    .orElseGet(() -> InventoryModel.builder()
                            .productId(productId)
                            .reservedStock(0)
                            .build());
        } else {
            inventory = InventoryModel.builder()
                    .reservedStock(0)
                    .build();
        }

        int reserved = inventory.getReservedStock() != null ? inventory.getReservedStock() : 0;
        if (requestDto.getQuantity() < reserved) {
            throw new InvalidReservationStateException(
                    String.format(
                            "No se puede fijar el stock a %d unidades porque hay %d unidades actualmente reservadas",
                            requestDto.getQuantity(), reserved));
        }

        inventory.setQuantity(requestDto.getQuantity());
        inventory.setDateUpdated(now);
        if (inventory.getReservedStock() == null) {
            inventory.setReservedStock(0);
        }

        InventoryModel saved = inventoryRepository.save(inventory);
        return InventoryResponseDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public ReserveResponseDTO reserveStock(ReserveRequestDTO requestDto) {
        // Bloqueo pesimista para evitar condiciones de carrera concurrentes
        InventoryModel inventory = inventoryRepository.findByIdForUpdate(requestDto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(requestDto.getProductId()));

        int totalStock = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int reservedStock = inventory.getReservedStock() != null ? inventory.getReservedStock() : 0;
        int availableStock = Math.max(0, totalStock - reservedStock);

        if (requestDto.getQuantity() > availableStock) {
            throw new InsufficientInventoryException(inventory.getProductId(), requestDto.getQuantity(),
                    availableStock);
        }

        // Actualizar stock reservado de la entidad
        inventory.setReservedStock(reservedStock + requestDto.getQuantity());
        inventory.setDateUpdated(LocalDateTime.now());
        inventoryRepository.save(inventory);

        // Crear registro de reserva en estado APPROVED
        ReserveInventory reservation = ReserveInventory.builder()
                .inventoryModel(inventory)
                .quantity(requestDto.getQuantity())
                .dateReserved(LocalDateTime.now())
                .status(ReserveState.APPROVED)
                .build();

        ReserveInventory savedReservation = reserveInventoryRepository.save(reservation);
        return ReserveResponseDTO.fromEntity(savedReservation);
    }

    @Override
    @Transactional(readOnly = true)
    public ReserveResponseDTO getReservationById(Long reserveId) {
        ReserveInventory reservation = reserveInventoryRepository.findById(reserveId)
                .orElseThrow(() -> new ReservationNotFoundException(reserveId));
        return ReserveResponseDTO.fromEntity(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReserveResponseDTO> getAllReservations() {
        return reserveInventoryRepository.findAll().stream()
                .map(ReserveResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReserveResponseDTO> getReservationsByProductId(Long productId) {
        return reserveInventoryRepository.findByInventoryModelProductId(productId).stream()
                .map(ReserveResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReserveResponseDTO releaseReservation(ReleaseReserveRequestDTO requestDto) {
        return releaseReservationById(requestDto.getReserveId());
    }

    @Override
    @Transactional
    public ReserveResponseDTO releaseReservationById(Long reserveId) {
        ReserveInventory reservation = reserveInventoryRepository.findById(reserveId)
                .orElseThrow(() -> new ReservationNotFoundException(reserveId));

        if (reservation.getStatus() == ReserveState.REJECTED) {
            throw new InvalidReservationStateException(
                    "La reserva con ID " + reserveId + " ya fue liberada o cancelada previamente");
        }

        InventoryModel inventory = reservation.getInventoryModel();
        if (inventory != null) {
            // Se bloquea el registro del inventario asociado antes de ajustar
            InventoryModel lockedInventory = inventoryRepository.findByIdForUpdate(inventory.getProductId())
                    .orElse(inventory);

            int currentReserved = lockedInventory.getReservedStock() != null ? lockedInventory.getReservedStock() : 0;
            int newReserved = Math.max(0, currentReserved - reservation.getQuantity());

            lockedInventory.setReservedStock(newReserved);
            lockedInventory.setDateUpdated(LocalDateTime.now());
            inventoryRepository.save(lockedInventory);
        }

        // Marcar reserva como REJECTED (compensación ejecutada)
        reservation.setStatus(ReserveState.REJECTED);
        ReserveInventory updatedReservation = reserveInventoryRepository.save(reservation);

        return ReserveResponseDTO.fromEntity(updatedReservation);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getAvailableStock(Long productId) {
        InventoryModel inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        int total = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int reserved = inventory.getReservedStock() != null ? inventory.getReservedStock() : 0;
        return Math.max(0, total - reserved);
    }
}