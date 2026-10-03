package com.Inventory_Service.demo.saga;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.Inventory_Service.demo.enums.ReserveState;
import com.Inventory_Service.demo.event.OrderCreatedPayload;
import com.Inventory_Service.demo.model.InventoryModel;
import com.Inventory_Service.demo.model.ReserveInventory;
import com.Inventory_Service.demo.publisher.InventoryEventPublisher;
import com.Inventory_Service.demo.repository.InventoryRepository;
import com.Inventory_Service.demo.repository.ReserveInventoryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SagaInventoryProcessor {

    private static final Logger log = LoggerFactory.getLogger(SagaInventoryProcessor.class);

    private final InventoryRepository inventoryRepository;
    private final ReserveInventoryRepository reserveInventoryRepository;
    private final InventoryEventPublisher eventPublisher;

    private final Set processedEventIds = ConcurrentHashMap.newKeySet();
    private final Map orderMetadataMap = new ConcurrentHashMap<>();
    private final Map orderToReserveMap = new ConcurrentHashMap<>();

    public boolean isDuplicateEvent(UUID eventId) {
        if (eventId == null) {
            return false;
        }
        boolean isNew = processedEventIds.add(eventId);
        if (!isNew) {
            log.warn("[Inventory Saga Idempotencia] Evento duplicado detectado y descartado | eventId={}", eventId);
            return true;
        }
        return false;
    }

    public void registerOrderMetadata(OrderCreatedPayload payload) {
        if (payload != null && payload.getOrderId() != null) {
            orderMetadataMap.put(payload.getOrderId(), payload);
            log.info("[Inventory Saga] Metadata de orden capturada | orderId={} | productId={} | cantidad={}",
                    payload.getOrderId(), payload.getProductId(), payload.getQuantity());
        }
    }

    @Transactional
    public void processPaymentCompleted(Long orderId, UUID eventId) {
        log.info("[Inventory Saga] Procesando cobro completado para orden | orderId={} | eventId={}", orderId, eventId);

        // 1. Idempotencia: Verificar si ya existe una reserva registrada para este
        // orderId
        if (orderToReserveMap.containsKey(orderId)) {
            Long existingReserveId = (Long) orderToReserveMap.get(orderId);
            if (existingReserveId != null) {
                log.warn(
                        "[Inventory Saga] Ya existe una reserva previa (#{}) para orderId={}. Reenviando confirmación.",
                        existingReserveId, orderId);
                Optional prevOpt = reserveInventoryRepository.findById(existingReserveId);
                if (prevOpt.isPresent()) {
                    ReserveInventory r = (ReserveInventory) prevOpt.get();
                    if (r.getStatus() == ReserveState.APPROVED) {
                        Long prodId = r.getInventoryModel() != null ? r.getInventoryModel().getProductId() : 1L;
                        eventPublisher.publishInventoryReserved(orderId, r.getReserveId(), prodId, r.getQuantity());
                    }
                }
            }
            return;
        }

        // 2. Extraer datos del pedido con casteo explícito
        OrderCreatedPayload orderData = (OrderCreatedPayload) orderMetadataMap.get(orderId);
        Long productId = (orderData != null && orderData.getProductId() != null) ? orderData.getProductId() : 1L;
        Integer quantity = (orderData != null && orderData.getQuantity() != null) ? orderData.getQuantity() : 1;

        if (quantity <= 0) {
            log.warn("[Inventory Saga Negocio] Cantidad solicitada inválida ({} <= 0) para orderId={}", quantity,
                    orderId);
            eventPublisher.publishInventoryReservationFailed(orderId, productId,
                    "Cantidad solicitada debe ser estrictamente positiva");
            return;
        }

        try {
            // 3. Bloqueo pesimista para evitar sobreventa ante concurrencia
            Optional optInventory = inventoryRepository.findByIdForUpdate(productId);
            if (optInventory.isEmpty()) {
                log.warn("[Inventory Saga Negocio] Producto ID {} inexistente en almacén", productId);
                eventPublisher.publishInventoryReservationFailed(orderId, productId,
                        "Producto no existe en inventario");
                return;
            }

            InventoryModel inventory = (InventoryModel) optInventory.get();
            int totalStock = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            int currentReserved = inventory.getReservedStock() != null ? inventory.getReservedStock() : 0;
            int availableStock = Math.max(0, totalStock - currentReserved);

            // 4. Verificación estricta de disponibilidad
            if (quantity > availableStock) {
                log.warn("[Inventory Saga Negocio] Stock insuficiente | productId={} | solicitado={} | disponible={}",
                        productId, quantity, availableStock);
                eventPublisher.publishInventoryReservationFailed(orderId, productId,
                        String.format("Stock insuficiente: solicitado %d, disponible %d", quantity, availableStock));
                return;
            }

            // 5. Descuento atómico del stock reservado
            inventory.setReservedStock(currentReserved + quantity);
            inventory.setDateUpdated(LocalDateTime.now());
            inventoryRepository.save(inventory);

            // 6. Registro de la reserva en base de datos local
            ReserveInventory reservation = ReserveInventory.builder()
                    .inventoryModel(inventory)
                    .quantity(quantity)
                    .dateReserved(LocalDateTime.now())
                    .status(ReserveState.APPROVED)
                    .build();

            ReserveInventory savedReserve = reserveInventoryRepository.save(reservation);
            orderToReserveMap.put(orderId, savedReserve.getReserveId());

            log.info(
                    "[Inventory Saga Success] Reserva aprobada | orderId={} | reserveId={} | productId={} | cantidad={}",
                    orderId, savedReserve.getReserveId(), productId, quantity);
            eventPublisher.publishInventoryReserved(orderId, savedReserve.getReserveId(), productId, quantity);

        } catch (DataAccessException ex) {
            log.error("[Inventory Saga Técnico] Error de base de datos al procesar reserva para orderId={}: {}",
                    orderId, ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public void processCompensateRelease(Long orderId, UUID eventId, String reason) {
        log.info("[Inventory Saga] Iniciando compensación (liberación) por fallo logístico | orderId={} | razón={}",
                orderId, reason);

        Long reserveId = (Long) orderToReserveMap.get(orderId);
        if (reserveId == null) {
            log.warn("[Inventory Saga] No se encontró reserva asociada al orderId={}. Se descarta liberación.",
                    orderId);
            return;
        }

        try {
            Optional optReserve = reserveInventoryRepository.findById(reserveId);
            if (optReserve.isEmpty()) {
                log.warn("[Inventory Saga] Reserva ID {} no encontrada para compensar", reserveId);
                return;
            }

            ReserveInventory reservation = (ReserveInventory) optReserve.get();

            // Idempotencia: no liberar dos veces una reserva cancelada
            if (reservation.getStatus() == ReserveState.REJECTED) {
                log.warn("[Inventory Saga Idempotencia] Reserva ID {} ya se encontraba liberada (REJECTED)", reserveId);
                return;
            }

            InventoryModel inventory = reservation.getInventoryModel();
            if (inventory != null) {
                Optional optLocked = inventoryRepository.findByIdForUpdate(inventory.getProductId());
                if (optLocked.isPresent()) {
                    InventoryModel lockedInv = (InventoryModel) optLocked.get();
                    int currentReserved = lockedInv.getReservedStock() != null ? lockedInv.getReservedStock() : 0;
                    int newReserved = Math.max(0, currentReserved - reservation.getQuantity());
                    lockedInv.setReservedStock(newReserved);
                    lockedInv.setDateUpdated(LocalDateTime.now());
                    inventoryRepository.save(lockedInv);
                }
            }

            reservation.setStatus(ReserveState.REJECTED);
            reserveInventoryRepository.save(reservation);

            log.info("[Inventory Saga Compensación] Reserva #{} liberada exitosamente para orderId={}", reserveId,
                    orderId);
            eventPublisher.publishInventoryReleased(orderId, reserveId);

        } catch (DataAccessException ex) {
            log.error("[Inventory Saga Técnico] Error de base de datos al liberar reserva #{} para orderId={}: {}",
                    reserveId, orderId, ex.getMessage());
            throw ex;
        }
    }
}