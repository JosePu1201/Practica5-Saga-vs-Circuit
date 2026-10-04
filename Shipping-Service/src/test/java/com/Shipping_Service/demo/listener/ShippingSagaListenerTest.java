package com.Shipping_Service.demo.listener;

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

import com.Shipping_Service.demo.config.RabbitConfig;
import com.Shipping_Service.demo.event.OrderCreatedPayload;
import com.Shipping_Service.demo.event.SagaEvent;
import com.Shipping_Service.demo.saga.SagaShippingProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ShippingSagaListenerTest {

    @Mock
    private SagaShippingProcessor sagaShippingProcessor;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Message message;

    @Mock
    private MessageProperties messageProperties;

    @InjectMocks
    private ShippingSagaListener listener;

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
    @DisplayName("onSagaEvent - Evento nulo o duplicado")
    void testNullOrDuplicateEvent() {
        listener.onSagaEvent(null, message);
        verify(sagaShippingProcessor, never()).isDuplicateEvent(any());

        SagaEvent<Object> event = createEvent(null);
        when(sagaShippingProcessor.isDuplicateEvent(eventId)).thenReturn(true);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_ORDER_CREATED);

        listener.onSagaEvent(event, message);

        verify(sagaShippingProcessor, never()).registerOrderMetadata(any());
    }

    @Test
    @DisplayName("onSagaEvent - RK_ORDER_CREATED")
    void testOrderCreated() {
        OrderCreatedPayload payload = OrderCreatedPayload.builder().orderId(100L).build();
        SagaEvent<Object> event = createEvent(payload);

        when(sagaShippingProcessor.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_ORDER_CREATED);

        listener.onSagaEvent(event, message);

        verify(sagaShippingProcessor).registerOrderMetadata(payload);
    }

    @Test
    @DisplayName("onSagaEvent - RK_INVENTORY_RESERVED")
    void testInventoryReserved() {
        SagaEvent<Object> event = createEvent(null);

        when(sagaShippingProcessor.isDuplicateEvent(eventId)).thenReturn(false);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getReceivedRoutingKey()).thenReturn(RabbitConfig.RK_INVENTORY_RESERVED);

        listener.onSagaEvent(event, message);

        verify(sagaShippingProcessor).processInventoryReserved(eq(100L), eq(eventId), any());
    }
}
