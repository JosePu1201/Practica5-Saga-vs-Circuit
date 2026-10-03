package com.Payment_Service.demo.publisher;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.Payment_Service.demo.config.RabbitConfig;
import com.Payment_Service.demo.event.SagaEvent;
import com.Payment_Service.demo.event.SagaStepResultPayload;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentCompleted(Long orderId, Long paymentId, Integer amount) {
        UUID eventId = UUID.randomUUID();
        SagaStepResultPayload payload = SagaStepResultPayload.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .status("SUCCESS")
                .details("Cobro completado por monto: Q" + amount)
                .build();

        publishEvent(RabbitConfig.RK_PAYMENT_COMPLETED, orderId, eventId, payload);
    }

    public void publishPaymentFailed(Long orderId, Long paymentId, String reason) {
        UUID eventId = UUID.randomUUID();
        SagaStepResultPayload payload = SagaStepResultPayload.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .status("FAILED")
                .reason(reason)
                .build();

        publishEvent(RabbitConfig.RK_PAYMENT_FAILED, orderId, eventId, payload);
    }

    public void publishPaymentRefunded(Long orderId, Long paymentId, Integer amount) {
        UUID eventId = UUID.randomUUID();
        SagaStepResultPayload payload = SagaStepResultPayload.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .status("CANCELLED")
                .details("Reembolso simulado ejecutado por monto: Q" + amount)
                .build();

        publishEvent(RabbitConfig.RK_PAYMENT_REFUNDED, orderId, eventId, payload);
    }

    private void publishEvent(String routingKey, Long orderId, UUID eventId, SagaStepResultPayload payload) {
        SagaEvent event = SagaEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .eventType(routingKey)
                .occurredAt(LocalDateTime.now())
                .payload(payload)
                .build();

        log.info("[Payment Publisher] Publicando {} | orderId={} | eventId={}", routingKey, orderId, eventId);
        rabbitTemplate.convertAndSend(RabbitConfig.SAGA_EXCHANGE, routingKey, event);
    }
}