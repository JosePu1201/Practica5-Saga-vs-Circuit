package com.Payment_Service.demo.saga;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.Payment_Service.demo.enums.StatusPayment;
import com.Payment_Service.demo.event.OrderCreatedPayload;
import com.Payment_Service.demo.model.PaymentsModel;
import com.Payment_Service.demo.publisher.PaymentEventPublisher;
import com.Payment_Service.demo.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SagaPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(SagaPaymentProcessor.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    private final Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();

    public boolean isDuplicateEvent(UUID eventId) {
        if (eventId == null) {
            return false;
        }
        boolean isNew = processedEventIds.add(eventId);
        if (!isNew) {
            log.warn("[Saga Idempotencia] Evento duplicado detectado y omitido en Payment Service | eventId={}",
                    eventId);
            return true;
        }
        return false;
    }

    @Transactional
    public void processOrderCreated(UUID eventId, OrderCreatedPayload payload) {
        if (payload == null || payload.getOrderId() == null) {
            log.error("[Payment Saga] Payload de OrderCreated inválido o sin orderId");
            return;
        }

        Long orderId = payload.getOrderId();
        Integer amount = payload.getTotal() != null ? payload.getTotal() : 0;

        // Idempotencia de negocio: verificar si ya existe un pago previo para este
        // orderId
        List<PaymentsModel> existingPayments = paymentRepository.findByOrderId(orderId);
        if (existingPayments != null && !existingPayments.isEmpty()) {
            PaymentsModel firstPayment = (PaymentsModel) existingPayments.get(0);
            log.warn("[Payment Saga] Ya existe un registro de pago para orderId={} | paymentId={}", orderId,
                    firstPayment.getId());
            if (firstPayment.getStatus() == StatusPayment.SUCCESS) {
                eventPublisher.publishPaymentCompleted(orderId, firstPayment.getId(), firstPayment.getAmount());
            } else if (firstPayment.getStatus() == StatusPayment.FAILED) {
                eventPublisher.publishPaymentFailed(orderId, firstPayment.getId(), "Cobro previamente fallido");
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentsModel payment = PaymentsModel.builder()
                .orderId(orderId)
                .amount(amount)
                .status(StatusPayment.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PaymentsModel savedPayment = paymentRepository.save(payment);

        // Regla determinista de simulación: total=9999 o customerId=99 simula fallo
        boolean simulateFailure = (payload.getTotal() != null && payload.getTotal() == 9999)
                || (payload.getCustomerId() != null && payload.getCustomerId() == 99L);

        if (simulateFailure) {
            savedPayment.setStatus(StatusPayment.FAILED);
            savedPayment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(savedPayment);

            log.warn("[Payment Saga] Cobro rechazado (simulación determinista) | orderId={} | paymentId={}", orderId,
                    savedPayment.getId());
            eventPublisher.publishPaymentFailed(orderId, savedPayment.getId(),
                    "Fondos insuficientes o límite de crédito excedido (Simulado)");
        } else {
            savedPayment.setStatus(StatusPayment.SUCCESS);
            savedPayment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(savedPayment);

            log.info("[Payment Saga] Cobro aprobado exitosamente | orderId={} | paymentId={} | monto={}", orderId,
                    savedPayment.getId(), amount);
            eventPublisher.publishPaymentCompleted(orderId, savedPayment.getId(), amount);
        }
    }

    @Transactional
    public void processCompensateRefund(Long orderId, UUID eventId, String reason) {
        log.info("[Payment Saga] Iniciando compensación (reembolso) para orderId={} | razón={}", orderId, reason);

        List<PaymentsModel> payments = paymentRepository.findByOrderId(orderId);
        if (payments == null || payments.isEmpty()) {
            log.warn("[Payment Saga] No se encontró pago para orderId={}. Se descarta reembolso ficticio.", orderId);
            return;
        }

        PaymentsModel payment = (PaymentsModel) payments.get(0);

        if (payment.getStatus() == StatusPayment.CANCELLED) {
            log.warn("[Payment Saga Idempotencia] El pago ya se encuentra reembolsado (CANCELLED) | paymentId={}",
                    payment.getId());
            return;
        }

        if (payment.getStatus() == StatusPayment.FAILED) {
            log.info(
                    "[Payment Saga] El pago falló originalmente (FAILED); no hay fondos cobrados que reembolsar | paymentId={}",
                    payment.getId());
            return;
        }

        if (payment.getStatus() == StatusPayment.SUCCESS || payment.getStatus() == StatusPayment.PENDING) {
            payment.setStatus(StatusPayment.CANCELLED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("[Payment Saga] Reembolso aplicado exitosamente | orderId={} | paymentId={}", orderId,
                    payment.getId());
            eventPublisher.publishPaymentRefunded(orderId, payment.getId(), payment.getAmount());
        }
    }
}