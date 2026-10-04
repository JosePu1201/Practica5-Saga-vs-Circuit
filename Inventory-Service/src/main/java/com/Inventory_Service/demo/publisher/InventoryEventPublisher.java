package com.Inventory_Service.demo.publisher;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.Inventory_Service.demo.config.RabbitConfig;
import com.Inventory_Service.demo.event.SagaEvent;
import com.Inventory_Service.demo.event.SagaStepResultPayload;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publishInventoryReserved(Long orderId, Long reserveId, Long productId, Integer quantity) {
        UUID eventId = UUID.randomUUID();
        SagaStepResultPayload payload = SagaStepResultPayload.builder()
                .orderId(orderId)
                .reserveId(reserveId)
                .status("APPROVED")
                .details(String.format("Reserva #%d aprobada para %d unidad(es) del producto ID %d", reserveId,
                        quantity, productId))
                .build();

        publishEvent(RabbitConfig.RK_INVENTORY_RESERVED, orderId, eventId, payload);
    }

    public void publishInventoryReservationFailed(Long orderId, Long productId, String reason) {
        UUID eventId = UUID.randomUUID();
        SagaStepResultPayload payload = SagaStepResultPayload.builder()
                .orderId(orderId)
                .status("REJECTED")
                .reason(reason)
                .details(String.format("Fallo al reservar producto ID %d para la orden %d", productId, orderId))
                .build();

        publishEvent(RabbitConfig.RK_INVENTORY_RESERVATION_FAILED, orderId, eventId, payload);
    }

    public void publishInventoryReleased(Long orderId, Long reserveId) {
        UUID eventId = UUID.randomUUID();
        SagaStepResultPayload payload = SagaStepResultPayload.builder()
                .orderId(orderId)
                .reserveId(reserveId)
                .status("REJECTED")
                .details(String.format("Compensación ejecutada: existencias liberadas para la reserva #%d", reserveId))
                .build();

        publishEvent(RabbitConfig.RK_INVENTORY_RELEASED, orderId, eventId, payload);
    }

    private void publishEvent(String routingKey, Long orderId, UUID eventId, SagaStepResultPayload payload) {
        SagaEvent event = SagaEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .eventType(routingKey)
                .occurredAt(LocalDateTime.now())
                .payload(payload)
                .build();

        log.info("[Inventory Publisher] Emitiendo {} | orderId={} | eventId={}", routingKey, orderId, eventId);
        rabbitTemplate.convertAndSend(RabbitConfig.SAGA_EXCHANGE, routingKey, event);
    }
}