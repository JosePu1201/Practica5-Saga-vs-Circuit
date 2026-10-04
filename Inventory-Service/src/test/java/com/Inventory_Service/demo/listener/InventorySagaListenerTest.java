package com.Inventory_Service.demo.listener;

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

import com.Inventory_Service.demo.config.RabbitConfig;
import com.Inventory_Service.demo.event.OrderCreatedPayload;
import com.Inventory_Service.demo.event.SagaEvent;
import com.Inventory_Service.demo.event.SagaStepResultPayload;
import com.Inventory_Service.demo.saga.SagaInventoryProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class InventorySagaListenerTest {

    @Mock
    private SagaInventoryProcessor sagaInventoryProcessor;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Message message;

    @Mock
    private MessageProperties messageProperties;

    @InjectMocks
    private InventorySagaListener listener;

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
    void testNullEvent() {
        listener.onSagaEvent(null, message);
        listener.onSagaEvent(new SagaEvent<>(), message);

        verify(sagaInventoryProcessor, never()).isDuplicateEvent(any());
    }

    @Test
    @DisplayName("onSagaEvent - Evento duplicado debe ignorarse")
    void testDuplicateEvent() {
        SagaEvent<Object> event = createEvent(null);
        when(sagaInventoryProcessor.isDuplicateEvent(eventId)).thenReturn(true);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_ORDER_CREATED);

        listener.onSagaEvent(event, message);

        verify(sagaInventoryProcessor, never()).registerOrderMetadata(any());
    }

    @Test
    @DisplayName("onSagaEvent - RK_ORDER_CREATED con payload directo")
    void testOrderCreatedDirectPayload() {
        OrderCreatedPayload payload = OrderCreatedPayload.builder().orderId(100L).productId(1L).quantity(2).build();
        SagaEvent<Object> event = createEvent(payload);

        when(sagaInventoryProcessor.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_ORDER_CREATED);

        listener.onSagaEvent(event, message);

        verify(sagaInventoryProcessor).registerOrderMetadata(payload);
    }

    @Test
    @DisplayName("onSagaEvent - RK_ORDER_CREATED con payload a convertir via ObjectMapper")
    void testOrderCreatedMapPayload() {
        String jsonPayload = "{\"orderId\":100, \"productId\":1, \"quantity\":2}";
        SagaEvent<Object> event = createEvent(jsonPayload);
        OrderCreatedPayload convertedPayload = OrderCreatedPayload.builder().orderId(100L).build();

        when(sagaInventoryProcessor.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_ORDER_CREATED);
        when(objectMapper.convertValue(eq(jsonPayload), eq(OrderCreatedPayload.class))).thenReturn(convertedPayload);

        listener.onSagaEvent(event, message);

        verify(sagaInventoryProcessor).registerOrderMetadata(convertedPayload);
    }

    @Test
    @DisplayName("onSagaEvent - RK_PAYMENT_COMPLETED")
    void testPaymentCompleted() {
        SagaEvent<Object> event = createEvent(null);

        when(sagaInventoryProcessor.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_PAYMENT_COMPLETED);

        listener.onSagaEvent(event, message);

        verify(sagaInventoryProcessor).processPaymentCompleted(100L, eventId);
    }

    @Test
    @DisplayName("onSagaEvent - RK_SHIPPING_FAILED (Trigger Compensación)")
    void testShippingFailed() {
        SagaStepResultPayload payload = new SagaStepResultPayload();
        payload.setReason("Dirección fuera de cobertura");
        SagaEvent<Object> event = createEvent(payload);

        when(sagaInventoryProcessor.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_SHIPPING_FAILED);

        listener.onSagaEvent(event, message);

        verify(sagaInventoryProcessor).processCompensateRelease(100L, eventId, "Dirección fuera de cobertura");
    }

    @Test
    @DisplayName("onSagaEvent - RoutingKey no mapeada para Inventory Service")
    void testUnmappedRoutingKey() {
        SagaEvent<Object> event = createEvent(null);

        when(sagaInventoryProcessor.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn("unmapped.routing.key");

        listener.onSagaEvent(event, message);

        verify(sagaInventoryProcessor, never()).processPaymentCompleted(any(), any());
    }
}
