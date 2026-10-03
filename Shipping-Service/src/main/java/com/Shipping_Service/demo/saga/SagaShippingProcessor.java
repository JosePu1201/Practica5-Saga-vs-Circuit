package com.Shipping_Service.demo.saga;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.Shipping_Service.demo.enums.ShipmentStatus;
import com.Shipping_Service.demo.event.OrderCreatedPayload;
import com.Shipping_Service.demo.model.ShipmentsModel;
import com.Shipping_Service.demo.publisher.ShippingEventPublisher;
import com.Shipping_Service.demo.repository.ShipmentsRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SagaShippingProcessor {

    private static final Logger log = LoggerFactory.getLogger(SagaShippingProcessor.class);

    private final ShipmentsRepository shipmentsRepository;
    private final ShippingEventPublisher eventPublisher;

    private final Set processedEventIds = ConcurrentHashMap.newKeySet();
    private final Map orderAddressMap = new ConcurrentHashMap<>();

    public boolean isDuplicateEvent(UUID eventId) {
        if (eventId == null) {
            return false;
        }
        boolean isNew = processedEventIds.add(eventId);
        if (!isNew) {
            log.warn("[Shipping Saga Idempotencia] Evento duplicado omitido | eventId={}", eventId);
            return true;
        }
        return false;
    }

    public void registerOrderMetadata(OrderCreatedPayload payload) {
        if (payload != null && payload.getOrderId() != null) {
            String address = (payload.getAddress() != null && !payload.getAddress().trim().isEmpty())
                    ? payload.getAddress().trim()
                    : "Dirección estándar de entrega";
            orderAddressMap.put(payload.getOrderId(), address);
            log.info("[Shipping Saga] Dirección capturada para orderId={}: {}", payload.getOrderId(), address);
        }
    }

    @Transactional
    public void processInventoryReserved(Long orderId, UUID eventId, Long reserveId) {
        log.info("[Shipping Saga] Procesando inventario reservado | orderId={} | reserveId={} | eventId={}",
                orderId, reserveId, eventId);

        // 1. Idempotencia: Verificar si ya existe un envío previo para este pedido
        List existingShipments = shipmentsRepository.findByOrderId(orderId);
        if (existingShipments != null && !existingShipments.isEmpty()) {
            ShipmentsModel firstShipment = (ShipmentsModel) existingShipments.get(0);
            log.warn("[Shipping Saga] Ya existe un envío registrado (#{}) para orderId={}. Reenviando evento.",
                    firstShipment.getShipmentId(), orderId);

            if (firstShipment.getStatus() == ShipmentStatus.PENDING
                    || firstShipment.getStatus() == ShipmentStatus.SHIPPED) {
                eventPublisher.publishShippingScheduled(orderId, firstShipment.getShipmentId(),
                        firstShipment.getAddress());
            } else if (firstShipment.getStatus() == ShipmentStatus.CANCELLED) {
                eventPublisher.publishShippingFailed(orderId, firstShipment.getShipmentId(),
                        "Envío previamente cancelado");
            }
            return;
        }

        // 2. Extraer o inferir dirección de entrega
        String address = (String) orderAddressMap.get(orderId);
        if (address == null || address.trim().isEmpty()) {
            address = "Dirección estándar del cliente";
        }

        // 3. Regla determinista de prueba para fallo logístico
        boolean simulateFailure = address.toUpperCase().contains("FAIL")
                || address.toUpperCase().contains("RECHAZO")
                || address.toUpperCase().contains("ERROR")
                || orderId == 9999L;

        try {
            LocalDateTime now = LocalDateTime.now();
            ShipmentsModel shipment = new ShipmentsModel();
            shipment.setOrderId(orderId);
            shipment.setAddress(address);
            shipment.setScheduleDate(now.plusDays(2));
            shipment.setCreatedAt(now);
            shipment.setUpdatedAt(now);

            if (simulateFailure) {
                shipment.setStatus(ShipmentStatus.CANCELLED);
                ShipmentsModel savedFailed = shipmentsRepository.save(shipment);

                log.warn(
                        "[Shipping Saga Negocio] Fallo en programación de envío (Simulado) | orderId={} | shipmentId={}",
                        orderId, savedFailed.getShipmentId());
                eventPublisher.publishShippingFailed(orderId, savedFailed.getShipmentId(),
                        "Dirección fuera del área de cobertura logística (Simulación determinista)");
            } else {
                shipment.setStatus(ShipmentStatus.PENDING);
                ShipmentsModel saved = shipmentsRepository.save(shipment);

                log.info("[Shipping Saga Success] Envío programado exitosamente | orderId={} | shipmentId={}",
                        orderId, saved.getShipmentId());
                eventPublisher.publishShippingScheduled(orderId, saved.getShipmentId(), address);
            }

        } catch (DataAccessException ex) {
            log.error(
                    "[Shipping Saga Técnico] Error de persistencia en base de datos al programar envío para orderId={}: {}",
                    orderId, ex.getMessage());
            throw ex;
        }
    }
}