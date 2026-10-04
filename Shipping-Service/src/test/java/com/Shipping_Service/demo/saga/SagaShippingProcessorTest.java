package com.Shipping_Service.demo.saga;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Shipping_Service.demo.enums.ShipmentStatus;
import com.Shipping_Service.demo.event.OrderCreatedPayload;
import com.Shipping_Service.demo.model.ShipmentsModel;
import com.Shipping_Service.demo.publisher.ShippingEventPublisher;
import com.Shipping_Service.demo.repository.ShipmentsRepository;

@ExtendWith(MockitoExtension.class)
class SagaShippingProcessorTest {

    @Mock
    private ShipmentsRepository shipmentsRepository;

    @Mock
    private ShippingEventPublisher eventPublisher;

    @InjectMocks
    private SagaShippingProcessor processor;

    private ShipmentsModel mockShipment;

    @BeforeEach
    void setUp() {
        mockShipment = new ShipmentsModel();
        mockShipment.setShipmentId(100L);
        mockShipment.setOrderId(10L);
        mockShipment.setAddress("Calle Test 123");
        mockShipment.setStatus(ShipmentStatus.PENDING);
    }

    @Test
    @DisplayName("isDuplicateEvent - Idempotencia de eventos")
    void testIsDuplicateEvent() {
        UUID id = UUID.randomUUID();
        assertFalse(processor.isDuplicateEvent(id));
        assertTrue(processor.isDuplicateEvent(id));
        assertFalse(processor.isDuplicateEvent(null));
    }

    @Test
    @DisplayName("registerOrderMetadata - Captura dirección del pedido")
    void testRegisterOrderMetadata() {
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .address("Av Central 456")
                .build();

        processor.registerOrderMetadata(payload);

        // Metadata nulo o sin orderId
        processor.registerOrderMetadata(null);
        processor.registerOrderMetadata(new OrderCreatedPayload());
    }

    @Test
    @DisplayName("processInventoryReserved - Programa envío exitoso")
    void testProcessInventoryReservedSuccess() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .address("Av Central 456")
                .build();
        processor.registerOrderMetadata(payload);

        when(shipmentsRepository.findByOrderId(10L)).thenReturn(List.of());
        when(shipmentsRepository.save(any(ShipmentsModel.class))).thenAnswer(i -> {
            ShipmentsModel s = i.getArgument(0);
            s.setShipmentId(100L);
            return s;
        });

        processor.processInventoryReserved(10L, eventId, 50L);

        verify(eventPublisher).publishShippingScheduled(eq(10L), eq(100L), eq("Av Central 456"));
    }

    @Test
    @DisplayName("processInventoryReserved - Fallo logístico por dirección o orderId especial")
    void testProcessInventoryReservedSimulatedFailure() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .address("Direccion FAIL")
                .build();
        processor.registerOrderMetadata(payload);

        when(shipmentsRepository.findByOrderId(10L)).thenReturn(List.of());
        when(shipmentsRepository.save(any(ShipmentsModel.class))).thenAnswer(i -> {
            ShipmentsModel s = i.getArgument(0);
            s.setShipmentId(100L);
            return s;
        });

        processor.processInventoryReserved(10L, eventId, 50L);

        verify(eventPublisher).publishShippingFailed(eq(10L), eq(100L), any());
    }

    @Test
    @DisplayName("processInventoryReserved - Reenvío si ya existe envío previo (Idempotencia)")
    void testProcessInventoryReservedExisting() {
        UUID eventId = UUID.randomUUID();
        when(shipmentsRepository.findByOrderId(10L)).thenReturn(List.of(mockShipment));

        processor.processInventoryReserved(10L, eventId, 50L);
        verify(eventPublisher).publishShippingScheduled(eq(10L), eq(100L), eq("Calle Test 123"));

        mockShipment.setStatus(ShipmentStatus.CANCELLED);
        processor.processInventoryReserved(10L, eventId, 50L);
        verify(eventPublisher).publishShippingFailed(eq(10L), eq(100L), any());
    }
}
