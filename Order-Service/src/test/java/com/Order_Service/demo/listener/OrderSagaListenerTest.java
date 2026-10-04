package com.Order_Service.demo.listener;

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

import com.Order_Service.demo.config.RabbitConfig;
import com.Order_Service.demo.event.SagaEvent;
import com.Order_Service.demo.event.SagaStepResultPayload;
import com.Order_Service.demo.saga.SagaOrderTracker;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderSagaListenerTest {

    @Mock
    private SagaOrderTracker sagaOrderTracker;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Message message;

    @Mock
    private MessageProperties messageProperties;

    @InjectMocks
    private OrderSagaListener listener;

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
    @DisplayName("onSagaEvent - Evento nulo o sin eventId debe ignorarse")
    void testNullEventOrEventId() {
        listener.onSagaEvent(null, message);
        listener.onSagaEvent(new SagaEvent<>(), message);

        verify(sagaOrderTracker, never()).isDuplicateEvent(any());
    }

    @Test
    @DisplayName("onSagaEvent - Evento duplicado debe detener la ejecución")
    void testDuplicateEvent() {
        SagaEvent<Object> event = createEvent(null);
        when(sagaOrderTracker.isDuplicateEvent(eventId)).thenReturn(true);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_PAYMENT_COMPLETED);

        listener.onSagaEvent(event, message);

        verify(sagaOrderTracker, never()).handlePaymentCompleted(any(), any());
    }

    @Test
    @DisplayName("onSagaEvent - RK_PAYMENT_COMPLETED")
    void testPaymentCompleted() {
        SagaEvent<Object> event = createEvent(null);
        when(sagaOrderTracker.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_PAYMENT_COMPLETED);

        listener.onSagaEvent(event, message);

        verify(sagaOrderTracker).handlePaymentCompleted(100L, eventId);
    }

    @Test
    @DisplayName("onSagaEvent - RK_PAYMENT_FAILED")
    void testPaymentFailed() {
        SagaStepResultPayload payload = new SagaStepResultPayload();
        payload.setReason("Fondos insuficientes");
        SagaEvent<Object> event = createEvent(payload);

        when(sagaOrderTracker.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_PAYMENT_FAILED);

        listener.onSagaEvent(event, message);

        verify(sagaOrderTracker).handlePaymentFailed(100L, eventId, "Fondos insuficientes");
    }

    @Test
    @DisplayName("onSagaEvent - RK_PAYMENT_REFUNDED")
    void testPaymentRefunded() {
        SagaEvent<Object> event = createEvent(null);
        when(sagaOrderTracker.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_PAYMENT_REFUNDED);

        listener.onSagaEvent(event, message);

        verify(sagaOrderTracker).handlePaymentRefunded(100L, eventId);
    }

    @Test
    @DisplayName("onSagaEvent - RK_INVENTORY_RESERVED")
    void testInventoryReserved() {
        SagaEvent<Object> event = createEvent(null);
        when(sagaOrderTracker.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_INVENTORY_RESERVED);

        listener.onSagaEvent(event, message);

        verify(sagaOrderTracker).handleInventoryReserved(100L, eventId);
    }

    @Test
    @DisplayName("onSagaEvent - RK_INVENTORY_RESERVATION_FAILED")
    void testInventoryFailed() {
        SagaStepResultPayload payload = new SagaStepResultPayload();
        payload.setReason("Sin stock");
        SagaEvent<Object> event = createEvent(payload);

        when(sagaOrderTracker.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_INVENTORY_RESERVATION_FAILED);

        listener.onSagaEvent(event, message);

        verify(sagaOrderTracker).handleInventoryFailed(100L, eventId, "Sin stock");
    }

    @Test
    @DisplayName("onSagaEvent - RK_INVENTORY_RELEASED")
    void testInventoryReleased() {
        SagaEvent<Object> event = createEvent(null);
        when(sagaOrderTracker.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_INVENTORY_RELEASED);

        listener.onSagaEvent(event, message);

        verify(sagaOrderTracker).handleInventoryReleased(100L, eventId);
    }

    @Test
    @DisplayName("onSagaEvent - RK_SHIPPING_SCHEDULED")
    void testShippingScheduled() {
        SagaEvent<Object> event = createEvent(null);
        when(sagaOrderTracker.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_SHIPPING_SCHEDULED);

        listener.onSagaEvent(event, message);

        verify(sagaOrderTracker).handleShippingScheduled(100L, eventId);
    }

    @Test
    @DisplayName("onSagaEvent - RK_SHIPPING_FAILED")
    void testShippingFailed() {
        SagaStepResultPayload payload = new SagaStepResultPayload();
        payload.setReason("Dirección inválida");
        SagaEvent<Object> event = createEvent(payload);

        when(sagaOrderTracker.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_SHIPPING_FAILED);

        listener.onSagaEvent(event, message);

        verify(sagaOrderTracker).handleShippingFailed(100L, eventId, "Dirección inválida");
    }

    @Test
    @DisplayName("onSagaEvent - RoutingKey desconocida")
    void testUnknownRoutingKey() {
        SagaEvent<Object> event = createEvent(null);
        when(sagaOrderTracker.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn("unknown.key");

        listener.onSagaEvent(event, message);

        verify(sagaOrderTracker, never()).handlePaymentCompleted(any(), any());
    }

    @Test
    @DisplayName("onSagaEvent - Conversion de raw payload via ObjectMapper")
    void testExtractPayloadConversion() {
        String jsonPayload = "{\"reason\":\"Detalle de error\"}";
        SagaEvent<Object> event = createEvent(jsonPayload);

        SagaStepResultPayload convertedPayload = new SagaStepResultPayload();
        convertedPayload.setReason("Detalle de error");

        when(sagaOrderTracker.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_PAYMENT_FAILED);
        when(objectMapper.convertValue(eq(jsonPayload), eq(SagaStepResultPayload.class))).thenReturn(convertedPayload);

        listener.onSagaEvent(event, message);

        verify(sagaOrderTracker).handlePaymentFailed(100L, eventId, "Detalle de error");
    }
}
