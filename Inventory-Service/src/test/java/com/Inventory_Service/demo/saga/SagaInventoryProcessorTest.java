package com.Inventory_Service.demo.saga;

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

import com.Inventory_Service.demo.enums.ReserveState;
import com.Inventory_Service.demo.event.OrderCreatedPayload;
import com.Inventory_Service.demo.model.InventoryModel;
import com.Inventory_Service.demo.model.ReserveInventory;
import com.Inventory_Service.demo.publisher.InventoryEventPublisher;
import com.Inventory_Service.demo.repository.InventoryRepository;
import com.Inventory_Service.demo.repository.ReserveInventoryRepository;

@ExtendWith(MockitoExtension.class)
class SagaInventoryProcessorTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ReserveInventoryRepository reserveInventoryRepository;

    @Mock
    private InventoryEventPublisher inventoryEventPublisher;

    @InjectMocks
    private SagaInventoryProcessor processor;

    private InventoryModel mockInventory;

    @BeforeEach
    void setUp() {
        mockInventory = new InventoryModel();
        mockInventory.setProductId(1L);
        mockInventory.setQuantity(100);
        mockInventory.setReservedStock(0);
    }

    @Test
    @DisplayName("isDuplicateEvent - Maneja correctamente duplicados")
    void testIsDuplicateEvent() {
        UUID id = UUID.randomUUID();
        assertFalse(processor.isDuplicateEvent(id));
        assertTrue(processor.isDuplicateEvent(id));
    }

    @Test
    @DisplayName("registerOrderMetadata - Almacena metadatos del pedido")
    void testRegisterOrderMetadata() {
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .productId(1L)
                .quantity(5)
                .build();

        processor.registerOrderMetadata(payload);
    }

    @Test
    @DisplayName("processPaymentCompleted - Reserva stock exitosamente y publica inventory.reserved")
    void testProcessPaymentCompletedSuccess() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .productId(1L)
                .quantity(5)
                .build();

        processor.registerOrderMetadata(payload);

        when(inventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockInventory));
        when(reserveInventoryRepository.save(any(ReserveInventory.class))).thenAnswer(i -> {
            ReserveInventory r = i.getArgument(0);
            r.setReserveId(100L);
            return r;
        });

        processor.processPaymentCompleted(10L, eventId);

        verify(inventoryRepository).save(mockInventory);
        verify(inventoryEventPublisher).publishInventoryReserved(10L, 100L, 1L, 5);
    }

    @Test
    @DisplayName("processPaymentCompleted - Insuficiente stock publica inventory.reservation.failed")
    void testProcessPaymentCompletedInsufficientStock() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .productId(1L)
                .quantity(500) // Excede 100 disponibles
                .build();

        processor.registerOrderMetadata(payload);

        when(inventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockInventory));

        processor.processPaymentCompleted(10L, eventId);

        verify(inventoryEventPublisher).publishInventoryReservationFailed(any(), any(), any());
    }

    @Test
    @DisplayName("processPaymentCompleted - Producto no encontrado en inventario")
    void testProcessPaymentCompletedProductNotFound() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .productId(99L)
                .quantity(5)
                .build();

        processor.registerOrderMetadata(payload);

        when(inventoryRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        processor.processPaymentCompleted(10L, eventId);

        verify(inventoryEventPublisher).publishInventoryReservationFailed(any(), any(), any());
    }

    @Test
    @DisplayName("processCompensateRelease - Libera reserva de inventario como acción compensatoria")
    void testProcessCompensateRelease() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(10L)
                .productId(1L)
                .quantity(5)
                .build();

        processor.registerOrderMetadata(payload);

        // Primero se simula reserva
        mockInventory.setReservedStock(5);
        when(inventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockInventory));
        ReserveInventory r = new ReserveInventory();
        r.setReserveId(50L);
        r.setQuantity(5);
        r.setInventoryModel(mockInventory);
        r.setStatus(ReserveState.APPROVED);

        when(reserveInventoryRepository.save(any(ReserveInventory.class))).thenReturn(r);
        processor.processPaymentCompleted(10L, eventId);

        when(reserveInventoryRepository.findById(50L)).thenReturn(Optional.of(r));

        // Ahora compensación
        processor.processCompensateRelease(10L, UUID.randomUUID(), "Fallo envío");

        verify(inventoryEventPublisher).publishInventoryReleased(10L, 50L);
    }

    @Test
    @DisplayName("processPaymentCompleted - Reserva repetida o ya existente en orderToReserveMap")
    void testProcessPaymentCompletedExistingReservation() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedPayload payload = OrderCreatedPayload.builder().orderId(10L).productId(1L).quantity(5).build();
        processor.registerOrderMetadata(payload);

        when(inventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockInventory));
        ReserveInventory r = new ReserveInventory();
        r.setReserveId(100L);
        r.setQuantity(5);
        r.setStatus(ReserveState.APPROVED);
        r.setInventoryModel(mockInventory);

        when(reserveInventoryRepository.save(any(ReserveInventory.class))).thenReturn(r);
        processor.processPaymentCompleted(10L, eventId);

        // Segunda llamada con la misma orden pero diferente eventId
        when(reserveInventoryRepository.findById(100L)).thenReturn(Optional.of(r));
        processor.processPaymentCompleted(10L, UUID.randomUUID());

        verify(inventoryEventPublisher, org.mockito.Mockito.times(2)).publishInventoryReserved(10L, 100L, 1L, 5);
    }

    @Test
    @DisplayName("processPaymentCompleted - Cantidad <= 0 debe publicar fallo")
    void testProcessPaymentCompletedInvalidQuantity() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedPayload payload = OrderCreatedPayload.builder().orderId(10L).productId(1L).quantity(-1).build();
        processor.registerOrderMetadata(payload);

        processor.processPaymentCompleted(10L, eventId);

        verify(inventoryEventPublisher).publishInventoryReservationFailed(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(1L), any());
    }

    @Test
    @DisplayName("processCompensateRelease - Reserva ya cancelada o no encontrada")
    void testProcessCompensateReleaseEdgeCases() {
        // Reserva no en mapa
        processor.processCompensateRelease(999L, UUID.randomUUID(), "Razón");

        // Reserva ya REJECTED
        OrderCreatedPayload payload = OrderCreatedPayload.builder().orderId(10L).productId(1L).quantity(5).build();
        processor.registerOrderMetadata(payload);

        when(inventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockInventory));
        ReserveInventory r = new ReserveInventory();
        r.setReserveId(50L);
        r.setQuantity(5);
        r.setInventoryModel(mockInventory);
        r.setStatus(ReserveState.APPROVED);

        when(reserveInventoryRepository.save(any(ReserveInventory.class))).thenReturn(r);
        processor.processPaymentCompleted(10L, UUID.randomUUID());

        r.setStatus(ReserveState.REJECTED);
        when(reserveInventoryRepository.findById(50L)).thenReturn(Optional.of(r));

        processor.processCompensateRelease(10L, UUID.randomUUID(), "Razón");
    }
}
