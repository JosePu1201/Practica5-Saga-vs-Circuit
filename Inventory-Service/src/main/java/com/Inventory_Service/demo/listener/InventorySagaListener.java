package com.Inventory_Service.demo.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.Inventory_Service.demo.config.RabbitConfig;
import com.Inventory_Service.demo.event.OrderCreatedPayload;
import com.Inventory_Service.demo.event.SagaEvent;
import com.Inventory_Service.demo.event.SagaStepResultPayload;
import com.Inventory_Service.demo.saga.SagaInventoryProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InventorySagaListener {

    private static final Logger log = LoggerFactory.getLogger(InventorySagaListener.class);

    private final SagaInventoryProcessor processor;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitConfig.INVENTORY_SAGA_QUEUE)
    public void onSagaEvent(SagaEvent event, Message message) {
        if (event == null || event.getEventId() == null) {
            log.error("[Inventory Listener] Evento malformado o sin eventId recibido");
            return;
        }

        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("[Inventory Listener] Evento recibido | routingKey={} | orderId={} | eventId={}",
                routingKey, event.getOrderId(), event.getEventId());

        if (processor.isDuplicateEvent(event.getEventId())) {
            return;
        }

        switch (routingKey) {
            case RabbitConfig.RK_ORDER_CREATED -> {
                OrderCreatedPayload payload = extractOrderCreatedPayload(event.getPayload());
                processor.registerOrderMetadata(payload);
            }
            case RabbitConfig.RK_PAYMENT_COMPLETED -> {
                processor.processPaymentCompleted(event.getOrderId(), event.getEventId());
            }
            case RabbitConfig.RK_SHIPPING_FAILED -> {
                SagaStepResultPayload payload = extractStepResultPayload(event.getPayload());
                String reason = payload != null && payload.getReason() != null ? payload.getReason()
                        : "Fallo en envío logístico";
                processor.processCompensateRelease(event.getOrderId(), event.getEventId(), reason);
            }
            default ->
                log.warn("[Inventory Listener] RoutingKey {} ignorada por Inventory Service", routingKey);
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