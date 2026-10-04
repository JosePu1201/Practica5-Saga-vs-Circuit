package com.Shipping_Service.demo.publisher;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.Shipping_Service.demo.config.RabbitConfig;
import com.Shipping_Service.demo.event.SagaEvent;
import com.Shipping_Service.demo.event.SagaStepResultPayload;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShippingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ShippingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publishShippingScheduled(Long orderId, Long shipmentId, String address) {
        UUID eventId = UUID.randomUUID();
        SagaStepResultPayload payload = SagaStepResultPayload.builder()
                .orderId(orderId)
                .shipmentId(shipmentId)
                .status("PENDING")
                .details("Envío programado exitosamente hacia: " + address)
                .build();

        publishEvent(RabbitConfig.RK_SHIPPING_SCHEDULED, orderId, eventId, payload);
    }

    public void publishShippingFailed(Long orderId, Long shipmentId, String reason) {
        UUID eventId = UUID.randomUUID();
        SagaStepResultPayload payload = SagaStepResultPayload.builder()
                .orderId(orderId)
                .shipmentId(shipmentId)
                .status("FAILED")
                .reason(reason)
                .details("Fallo en programación logística para el pedido " + orderId)
                .build();

        publishEvent(RabbitConfig.RK_SHIPPING_FAILED, orderId, eventId, payload);
    }

    private void publishEvent(String routingKey, Long orderId, UUID eventId, SagaStepResultPayload payload) {
        SagaEvent event = SagaEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .eventType(routingKey)
                .occurredAt(LocalDateTime.now())
                .payload(payload)
                .build();

        log.info("[Shipping Publisher] Publicando {} | orderId={} | eventId={}", routingKey, orderId, eventId);
        rabbitTemplate.convertAndSend(RabbitConfig.SAGA_EXCHANGE, routingKey, event);
    }
}