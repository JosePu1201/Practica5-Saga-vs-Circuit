package com.Order_Service.demo.saga;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Order_Service.demo.enums.StatusOrder;
import com.Order_Service.demo.model.OrdersModel;
import com.Order_Service.demo.repository.OrdersRepository;

@ExtendWith(MockitoExtension.class)
class SagaOrderTrackerTest {

    @Mock
    private OrdersRepository ordersRepository;

    @InjectMocks
    private SagaOrderTracker sagaOrderTracker;

    private OrdersModel mockOrder;

    @BeforeEach
    void setUp() {
        mockOrder = new OrdersModel();
        mockOrder.setId(10L);
        mockOrder.setStatus(StatusOrder.PENDING);
    }

    @Test
    @DisplayName("isDuplicateEvent - Debe detectar correctamente eventos duplicados")
    void testIsDuplicateEvent() {
        UUID eventId = UUID.randomUUID();

        // Primera vez -> No es duplicado
        assertFalse(sagaOrderTracker.isDuplicateEvent(eventId));

        // Segunda vez -> Es duplicado
        assertTrue(sagaOrderTracker.isDuplicateEvent(eventId));
    }

    @Test
    @DisplayName("evaluateCompletion - Pasa a COMPLETED cuando Payment, Inventory y Shipping han sido confirmados")
    void testEvaluateCompletionAllStepsConfirmed() {
        when(ordersRepository.findById(10L)).thenReturn(Optional.of(mockOrder));

        UUID event1 = UUID.randomUUID();
        UUID event2 = UUID.randomUUID();
        UUID event3 = UUID.randomUUID();

        sagaOrderTracker.handlePaymentCompleted(10L, event1);
        sagaOrderTracker.handleInventoryReserved(10L, event2);
        sagaOrderTracker.handleShippingScheduled(10L, event3);

        verify(ordersRepository).save(mockOrder);
    }

    @Test
    @DisplayName("handlePaymentFailed - Cancela la orden al recibir fallo de pago")
    void testHandlePaymentFailed() {
        when(ordersRepository.findById(10L)).thenReturn(Optional.of(mockOrder));

        UUID eventId = UUID.randomUUID();
        sagaOrderTracker.handlePaymentFailed(10L, eventId, "Fondos insuficientes");

        verify(ordersRepository).save(mockOrder);
    }

    @Test
    @DisplayName("handleShippingFailed - Cancela la orden al recibir fallo de envío")
    void testHandleShippingFailed() {
        when(ordersRepository.findById(10L)).thenReturn(Optional.of(mockOrder));

        UUID eventId = UUID.randomUUID();
        sagaOrderTracker.handleShippingFailed(10L, eventId, "Dirección no encontrada");

        verify(ordersRepository).save(mockOrder);
    }

    @Test
    @DisplayName("handlePaymentRefunded - Confirma cancelación tras reembolso")
    void testHandlePaymentRefunded() {
        when(ordersRepository.findById(10L)).thenReturn(Optional.of(mockOrder));

        UUID eventId = UUID.randomUUID();
        sagaOrderTracker.handlePaymentRefunded(10L, eventId);

        verify(ordersRepository).save(mockOrder);
    }

    @Test
    @DisplayName("evaluateCompletion - No cambia estado si la orden ya estaba CANCELLED")
    void testEvaluateCompletionAlreadyCancelled() {
        mockOrder.setStatus(StatusOrder.CANCELLED);
        when(ordersRepository.findById(10L)).thenReturn(Optional.of(mockOrder));

        UUID event1 = UUID.randomUUID();
        UUID event2 = UUID.randomUUID();
        UUID event3 = UUID.randomUUID();

        sagaOrderTracker.handlePaymentCompleted(10L, event1);
        sagaOrderTracker.handleInventoryReserved(10L, event2);
        sagaOrderTracker.handleShippingScheduled(10L, event3);

        verify(ordersRepository, never()).save(any());
    }
}
