package com.Order_Service.demo.saga;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.Order_Service.demo.enums.StatusOrder;
import com.Order_Service.demo.model.OrdersModel;
import com.Order_Service.demo.repository.OrdersRepository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SagaOrderTracker {

    private static final Logger log = LoggerFactory.getLogger(SagaOrderTracker.class);

    private final OrdersRepository ordersRepository;

    private final Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();
    private final Map<Long, OrderSagaProgress> orderProgressMap = new ConcurrentHashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSagaProgress {
        @Builder.Default
        private boolean paymentCompleted = false;
        @Builder.Default
        private boolean paymentFailed = false;
        @Builder.Default
        private boolean paymentRefunded = false;
        @Builder.Default
        private boolean inventoryReserved = false;
        @Builder.Default
        private boolean inventoryFailed = false;
        @Builder.Default
        private boolean inventoryReleased = false;
        @Builder.Default
        private boolean shippingScheduled = false;
        @Builder.Default
        private boolean shippingFailed = false;
    }

    public boolean isDuplicateEvent(UUID eventId) {
        if (eventId == null) {
            return false;
        }
        boolean isNew = processedEventIds.add(eventId);
        if (!isNew) {
            log.warn("[Saga Idempotencia] Evento duplicado detectado y omitido | eventId={}", eventId);
            return true;
        }
        return false;
    }

    // Solución al error: verificación explícita sin fallos de inferencia lambda
    public OrderSagaProgress getOrCreateProgress(Long orderId) {
        if (orderId == null) {
            return new OrderSagaProgress();
        }
        OrderSagaProgress progress = orderProgressMap.get(orderId);
        if (progress == null) {
            progress = new OrderSagaProgress();
            OrderSagaProgress existing = orderProgressMap.putIfAbsent(orderId, progress);
            if (existing != null) {
                progress = existing;
            }
        }
        return progress;
    }

    @Transactional
    public void handlePaymentCompleted(Long orderId, UUID eventId) {
        log.info("[Saga Step] Pago completado | orderId={} | eventId={}", orderId, eventId);
        OrderSagaProgress progress = getOrCreateProgress(orderId);
        progress.setPaymentCompleted(true);
        evaluateCompletion(orderId);
    }

    @Transactional
    public void handlePaymentFailed(Long orderId, UUID eventId, String reason) {
        log.warn("[Saga Step] Pago falló | orderId={} | eventId={} | razón={}", orderId, eventId, reason);
        OrderSagaProgress progress = getOrCreateProgress(orderId);
        progress.setPaymentFailed(true);
        cancelOrder(orderId, "Fallo en transacción de cobro: " + reason);
    }

    @Transactional
    public void handlePaymentRefunded(Long orderId, UUID eventId) {
        log.info("[Saga Step] Reembolso confirmado | orderId={} | eventId={}", orderId, eventId);
        OrderSagaProgress progress = getOrCreateProgress(orderId);
        progress.setPaymentRefunded(true);
        cancelOrder(orderId, "Compensación de pago (reembolso) completada");
    }

    @Transactional
    public void handleInventoryReserved(Long orderId, UUID eventId) {
        log.info("[Saga Step] Reserva de inventario completada | orderId={} | eventId={}", orderId, eventId);
        OrderSagaProgress progress = getOrCreateProgress(orderId);
        progress.setInventoryReserved(true);
        evaluateCompletion(orderId);
    }

    @Transactional
    public void handleInventoryFailed(Long orderId, UUID eventId, String reason) {
        log.warn("[Saga Step] Falló inventario | orderId={} | eventId={} | razón={}", orderId, eventId, reason);
        OrderSagaProgress progress = getOrCreateProgress(orderId);
        progress.setInventoryFailed(true);
        cancelOrder(orderId, "Inventario no disponible: " + reason);
    }

    @Transactional
    public void handleInventoryReleased(Long orderId, UUID eventId) {
        log.info("[Saga Step] Inventario liberado | orderId={} | eventId={}", orderId, eventId);
        OrderSagaProgress progress = getOrCreateProgress(orderId);
        progress.setInventoryReleased(true);
        cancelOrder(orderId, "Compensación de inventario completada");
    }

    @Transactional
    public void handleShippingScheduled(Long orderId, UUID eventId) {
        log.info("[Saga Step] Envío programado | orderId={} | eventId={}", orderId, eventId);
        OrderSagaProgress progress = getOrCreateProgress(orderId);
        progress.setShippingScheduled(true);
        evaluateCompletion(orderId);
    }

    @Transactional
    public void handleShippingFailed(Long orderId, UUID eventId, String reason) {
        log.warn("[Saga Step] Envío falló | orderId={} | eventId={} | razón={}", orderId, eventId, reason);
        OrderSagaProgress progress = getOrCreateProgress(orderId);
        progress.setShippingFailed(true);
        cancelOrder(orderId, "Fallo en despacho: " + reason);
    }

    private void evaluateCompletion(Long orderId) {
        OrderSagaProgress progress = getOrCreateProgress(orderId);

        if (progress.isPaymentCompleted() && progress.isInventoryReserved() && progress.isShippingScheduled()) {
            ordersRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() == StatusOrder.CANCELLED) {
                    log.warn("[Saga] Orden {} ya cancelada; no se pasa a COMPLETED", orderId);
                    return;
                }
                if (order.getStatus() == StatusOrder.COMPLETED) {
                    return;
                }

                order.setStatus(StatusOrder.COMPLETED);
                order.setUpdatedAt(LocalDateTime.now());
                ordersRepository.save(order);

                log.info("[Saga Success] TODAS las etapas confirmadas. Orden {} -> COMPLETED", orderId);
            });
        }
    }

    private void cancelOrder(Long orderId, String reason) {
        ordersRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == StatusOrder.CANCELLED) {
                return;
            }
            order.setStatus(StatusOrder.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            ordersRepository.save(order);
            log.warn("[Saga Compensación] Pedido {} marcado como CANCELLED | motivo: {}", orderId, reason);
        });
    }
}