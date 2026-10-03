package com.Shipping_Service.demo.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.Shipping_Service.demo.config.RabbitConfig;
import com.Shipping_Service.demo.event.OrderCreatedPayload;
import com.Shipping_Service.demo.event.SagaEvent;
import com.Shipping_Service.demo.event.SagaStepResultPayload;
import com.Shipping_Service.demo.saga.SagaShippingProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShippingSagaListener {

    private static final Logger log = LoggerFactory.getLogger(ShippingSagaListener.class);

    private final SagaShippingProcessor processor;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitConfig.SHIPPING_SAGA_QUEUE)
    public void onSagaEvent(SagaEvent event, Message message) {
        if (event == null || event.getEventId() == null) {
            log.error("[Shipping Listener] Evento malformado o sin eventId recibido");
            return;
        }

        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("[Shipping Listener] Evento recibido | routingKey={} | orderId={} | eventId={}",
                routingKey, event.getOrderId(), event.getEventId());

        if (processor.isDuplicateEvent(event.getEventId())) {
            return;
        }

        switch (routingKey) {
            case RabbitConfig.RK_ORDER_CREATED -> {
                OrderCreatedPayload payload = extractOrderCreatedPayload(event.getPayload());
                processor.registerOrderMetadata(payload);
            }
            case RabbitConfig.RK_INVENTORY_RESERVED -> {
                SagaStepResultPayload payload = extractStepResultPayload(event.getPayload());
                Long reserveId = payload != null ? payload.getReserveId() : null;
                processor.processInventoryReserved(event.getOrderId(), event.getEventId(), reserveId);
            }
            default ->
                log.warn("[Shipping Listener] RoutingKey {} ignorada por Shipping Service", routingKey);
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