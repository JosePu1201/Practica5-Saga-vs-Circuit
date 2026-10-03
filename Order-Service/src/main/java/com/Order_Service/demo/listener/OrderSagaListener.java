package com.Order_Service.demo.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.Order_Service.demo.config.RabbitConfig;
import com.Order_Service.demo.event.SagaEvent;
import com.Order_Service.demo.event.SagaStepResultPayload;
import com.Order_Service.demo.saga.SagaOrderTracker;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderSagaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaListener.class);

    private final SagaOrderTracker sagaOrderTracker;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitConfig.ORDER_SAGA_QUEUE)
    public void onSagaEvent(SagaEvent event, Message message) {
        if (event == null || event.getEventId() == null) {
            log.error("[Saga Listener] Mensaje malformado o sin eventId recibido");
            return;
        }

        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("[Saga Listener] Evento recibido | routingKey={} | orderId={} | eventId={}",
                routingKey, event.getOrderId(), event.getEventId());

        if (sagaOrderTracker.isDuplicateEvent(event.getEventId())) {
            return;
        }

        Long orderId = event.getOrderId();
        SagaStepResultPayload payload = extractPayload(event.getPayload());
        String reason = payload != null && payload.getReason() != null ? payload.getReason() : "Sin detalle";

        switch (routingKey) {
            case RabbitConfig.RK_PAYMENT_COMPLETED ->
                sagaOrderTracker.handlePaymentCompleted(orderId, event.getEventId());

            case RabbitConfig.RK_PAYMENT_FAILED ->
                sagaOrderTracker.handlePaymentFailed(orderId, event.getEventId(), reason);

            case RabbitConfig.RK_PAYMENT_REFUNDED ->
                sagaOrderTracker.handlePaymentRefunded(orderId, event.getEventId());

            case RabbitConfig.RK_INVENTORY_RESERVED ->
                sagaOrderTracker.handleInventoryReserved(orderId, event.getEventId());

            case RabbitConfig.RK_INVENTORY_RESERVATION_FAILED ->
                sagaOrderTracker.handleInventoryFailed(orderId, event.getEventId(), reason);

            case RabbitConfig.RK_INVENTORY_RELEASED ->
                sagaOrderTracker.handleInventoryReleased(orderId, event.getEventId());

            case RabbitConfig.RK_SHIPPING_SCHEDULED ->
                sagaOrderTracker.handleShippingScheduled(orderId, event.getEventId());

            case RabbitConfig.RK_SHIPPING_FAILED ->
                sagaOrderTracker.handleShippingFailed(orderId, event.getEventId(), reason);

            default ->
                log.warn("[Saga Listener] RoutingKey no reconocida en Order Service: {}", routingKey);
        }
    }

    private SagaStepResultPayload extractPayload(Object rawPayload) {
        if (rawPayload == null) {
            return null;
        }
        if (rawPayload instanceof SagaStepResultPayload stepPayload) {
            return stepPayload;
        }
        return objectMapper.convertValue(rawPayload, SagaStepResultPayload.class);
    }
}