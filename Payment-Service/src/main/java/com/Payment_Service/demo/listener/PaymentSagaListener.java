package com.Payment_Service.demo.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.Payment_Service.demo.config.RabbitConfig;
import com.Payment_Service.demo.event.OrderCreatedPayload;
import com.Payment_Service.demo.event.SagaEvent;
import com.Payment_Service.demo.event.SagaStepResultPayload;
import com.Payment_Service.demo.saga.SagaPaymentProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentSagaListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaListener.class);

    private final SagaPaymentProcessor paymentProcessor;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitConfig.PAYMENT_SAGA_QUEUE)
    public void onSagaEvent(SagaEvent event, Message message) {
        if (event == null || event.getEventId() == null) {
            log.error("[Payment Listener] Mensaje malformado o sin eventId recibido");
            return;
        }

        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("[Payment Listener] Mensaje recibido | routingKey={} | orderId={} | eventId={}",
                routingKey, event.getOrderId(), event.getEventId());

        if (paymentProcessor.isDuplicateEvent(event.getEventId())) {
            return;
        }

        switch (routingKey) {
            case RabbitConfig.RK_ORDER_CREATED -> {
                OrderCreatedPayload payload = extractOrderCreatedPayload(event.getPayload());
                paymentProcessor.processOrderCreated(event.getEventId(), payload);
            }
            case RabbitConfig.RK_INVENTORY_RESERVATION_FAILED -> {
                SagaStepResultPayload payload = extractStepResultPayload(event.getPayload());
                String reason = payload != null && payload.getReason() != null ? payload.getReason()
                        : "Inventario insuficiente";
                paymentProcessor.processCompensateRefund(event.getOrderId(), event.getEventId(), reason);
            }
            case RabbitConfig.RK_SHIPPING_FAILED -> {
                SagaStepResultPayload payload = extractStepResultPayload(event.getPayload());
                String reason = payload != null && payload.getReason() != null ? payload.getReason()
                        : "Fallo en despacho logístico";
                paymentProcessor.processCompensateRefund(event.getOrderId(), event.getEventId(), reason);
            }
            default ->
                log.warn("[Payment Listener] RoutingKey {} ignorada por Payment Service", routingKey);
        }
    }

    private OrderCreatedPayload extractOrderCreatedPayload(Object rawPayload) {
        if (rawPayload == null)
            return null;
        if (rawPayload instanceof OrderCreatedPayload payload)
            return payload;
        return objectMapper.convertValue(rawPayload, OrderCreatedPayload.class);
    }

    private SagaStepResultPayload extractStepResultPayload(Object rawPayload) {
        if (rawPayload == null)
            return null;
        if (rawPayload instanceof SagaStepResultPayload payload)
            return payload;
        return objectMapper.convertValue(rawPayload, SagaStepResultPayload.class);
    }
}