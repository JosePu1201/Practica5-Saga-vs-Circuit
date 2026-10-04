package com.Order_Service.demo.publisher;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.Order_Service.demo.config.RabbitConfig;
import com.Order_Service.demo.event.OrderCreatedPayload;
import com.Order_Service.demo.event.SagaEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedPayload payload) {
        UUID eventId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        SagaEvent event = SagaEvent.builder()
                .eventId(eventId)
                .orderId(payload.getOrderId())
                .eventType(RabbitConfig.RK_ORDER_CREATED)
                .occurredAt(now)
                .payload(payload)
                .build();

        log.info("[Saga Publisher] Emitiendo evento {} | orderId={} | eventId={}",
                RabbitConfig.RK_ORDER_CREATED, payload.getOrderId(), eventId);

        rabbitTemplate.convertAndSend(RabbitConfig.SAGA_EXCHANGE, RabbitConfig.RK_ORDER_CREATED, event);

        log.info("[Saga Publisher] Evento {} despachado a RabbitMQ | orderId={} | eventId={}",
                RabbitConfig.RK_ORDER_CREATED, payload.getOrderId(), eventId);
    }
}