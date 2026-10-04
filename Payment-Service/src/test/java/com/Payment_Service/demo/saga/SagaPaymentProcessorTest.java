package com.Payment_Service.demo.saga;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Payment_Service.demo.enums.StatusPayment;
import com.Payment_Service.demo.event.OrderCreatedPayload;
import com.Payment_Service.demo.model.PaymentsModel;
import com.Payment_Service.demo.publisher.PaymentEventPublisher;
import com.Payment_Service.demo.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class SagaPaymentProcessorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @InjectMocks
    private SagaPaymentProcessor processor;

    private PaymentsModel mockPayment;

    @BeforeEach
    void setUp() {
        mockPayment = PaymentsModel.builder()
                .id(1L)
                .orderId(10L)
                .amount(100)
                .status(StatusPayment.SUCCESS)
                .build();
    }

    @Test
    @DisplayName("isDuplicateEvent - Idempotencia de evento")
    void testIsDuplicateEvent() {
        UUID id = UUID.randomUUID();
        assertFalse(processor.isDuplicateEvent(id));
        assertTrue(processor.isDuplicateEvent(id));
        assertFalse(processor.isDuplicateEvent(null));
    }

    @Test
    @DisplayName("processOrderCreated - Payload nulo o sin orderId")
    void testProcessOrderCreatedNullPayload() {
        processor.processOrderCreated(UUID.randomUUID(), null);
        processor.processOrderCreated(UUID.randomUUID(), new OrderCreatedPayload());

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("processOrderCreated - Pago exitoso (monto <= 500)")
    void testProcessOrderCreatedSuccess() {
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .total(100)
                .customerId(1L)
                .build();

        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of());
        when(paymentRepository.save(any(PaymentsModel.class))).thenAnswer(i -> {
            PaymentsModel p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        processor.processOrderCreated(UUID.randomUUID(), payload);

        verify(eventPublisher).publishPaymentCompleted(eq(10L), eq(1L), eq(100));
    }

    @Test
    @DisplayName("processOrderCreated - Pago fallido por simulación (monto > 500)")
    void testProcessOrderCreatedFailedAmount() {
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .total(600)
                .customerId(1L)
                .build();

        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of());
        when(paymentRepository.save(any(PaymentsModel.class))).thenAnswer(i -> {
            PaymentsModel p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        processor.processOrderCreated(UUID.randomUUID(), payload);

        verify(eventPublisher).publishPaymentFailed(eq(10L), eq(1L), any());
    }

    @Test
    @DisplayName("processOrderCreated - Pago fallido por cliente 99")
    void testProcessOrderCreatedFailedCustomer() {
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .total(100)
                .customerId(99L)
                .build();

        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of());
        when(paymentRepository.save(any(PaymentsModel.class))).thenAnswer(i -> {
            PaymentsModel p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        processor.processOrderCreated(UUID.randomUUID(), payload);

        verify(eventPublisher).publishPaymentFailed(eq(10L), eq(1L), any());
    }

    @Test
    @DisplayName("processOrderCreated - Idempotencia de pago ya existente")
    void testProcessOrderCreatedExisting() {
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .total(100)
                .build();

        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of(mockPayment));

        processor.processOrderCreated(UUID.randomUUID(), payload);

        verify(eventPublisher).publishPaymentCompleted(eq(10L), eq(1L), eq(100));
    }

    @Test
    @DisplayName("processCompensateRefund - Reembolso exitoso")
    void testProcessCompensateRefundSuccess() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of(mockPayment));

        processor.processCompensateRefund(10L, UUID.randomUUID(), "Fallo inventario");

        verify(paymentRepository).save(mockPayment);
        verify(eventPublisher).publishPaymentRefunded(eq(10L), eq(1L), eq(100));
    }

    @Test
    @DisplayName("processCompensateRefund - Casos borde (pago no existe, ya CANCELLED, o FAILED)")
    void testProcessCompensateRefundEdgeCases() {
        // No existe pago
        when(paymentRepository.findByOrderId(99L)).thenReturn(List.of());
        processor.processCompensateRefund(99L, UUID.randomUUID(), "Razón");

        // Pago ya cancelado
        mockPayment.setStatus(StatusPayment.CANCELLED);
        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of(mockPayment));
        processor.processCompensateRefund(10L, UUID.randomUUID(), "Razón");

        // Pago fallido
        mockPayment.setStatus(StatusPayment.FAILED);
        processor.processCompensateRefund(10L, UUID.randomUUID(), "Razón");
    }
}
