package com.Payment_Service.demo.listener;

import java.util.UUID;

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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.Payment_Service.demo.config.RabbitConfig;
import com.Payment_Service.demo.event.OrderCreatedPayload;
import com.Payment_Service.demo.event.SagaEvent;
import com.Payment_Service.demo.event.SagaStepResultPayload;
import com.Payment_Service.demo.saga.SagaPaymentProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentSagaListenerTest {

    @Mock
    private SagaPaymentProcessor sagaPaymentProcessor;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Message message;

    @Mock
    private MessageProperties messageProperties;

    @InjectMocks
    private PaymentSagaListener listener;

    private UUID eventId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
    }

    private SagaEvent<Object> createEvent(Object payload) {
        SagaEvent<Object> event = new SagaEvent<>();
        event.setEventId(eventId);
        event.setOrderId(100L);
        event.setPayload(payload);
        return event;
    }

    @Test
    @DisplayName("onSagaEvent - Evento nulo")
    void testNullEvent() {
        listener.onSagaEvent(null, message);
        verify(sagaPaymentProcessor, never()).isDuplicateEvent(any());
    }

    @Test
    @DisplayName("onSagaEvent - Evento duplicado")
    void testDuplicateEvent() {
        SagaEvent<Object> event = createEvent(null);
        when(sagaPaymentProcessor.isDuplicateEvent(eventId)).thenReturn(true);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_ORDER_CREATED);

        listener.onSagaEvent(event, message);

        verify(sagaPaymentProcessor, never()).processOrderCreated(any(), any());
    }

    @Test
    @DisplayName("onSagaEvent - RK_ORDER_CREATED")
    void testOrderCreated() {
        OrderCreatedPayload payload = OrderCreatedPayload.builder().orderId(100L).build();
        SagaEvent<Object> event = createEvent(payload);

        when(sagaPaymentProcessor.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_ORDER_CREATED);

        listener.onSagaEvent(event, message);

        verify(sagaPaymentProcessor).processOrderCreated(eq(eventId), any());
    }

    @Test
    @DisplayName("onSagaEvent - RK_INVENTORY_RESERVATION_FAILED (Compensación)")
    void testInventoryFailed() {
        SagaStepResultPayload payload = new SagaStepResultPayload();
        payload.setReason("Stock insuficiente");
        SagaEvent<Object> event = createEvent(payload);

        when(sagaPaymentProcessor.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_INVENTORY_RESERVATION_FAILED);

        listener.onSagaEvent(event, message);

        verify(sagaPaymentProcessor).processCompensateRefund(eq(100L), eq(eventId), eq("Stock insuficiente"));
    }

    @Test
    @DisplayName("onSagaEvent - RK_SHIPPING_FAILED (Compensación)")
    void testShippingFailed() {
        SagaStepResultPayload payload = new SagaStepResultPayload();
        payload.setReason("Error en guía de despacho");
        SagaEvent<Object> event = createEvent(payload);

        when(sagaPaymentProcessor.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_SHIPPING_FAILED);

        listener.onSagaEvent(event, message);

        verify(sagaPaymentProcessor).processCompensateRefund(eq(100L), eq(eventId), eq("Error en guía de despacho"));
    }
}
