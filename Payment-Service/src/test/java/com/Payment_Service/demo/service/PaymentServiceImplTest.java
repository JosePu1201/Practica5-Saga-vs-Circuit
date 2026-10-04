package com.Payment_Service.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Payment_Service.demo.dto.PaymentRequestDTO;
import com.Payment_Service.demo.dto.PaymentResponseDTO;
import com.Payment_Service.demo.enums.StatusPayment;
import com.Payment_Service.demo.exception.InvalidPaymentStateException;
import com.Payment_Service.demo.exception.PaymentNotFoundException;
import com.Payment_Service.demo.model.PaymentsModel;
import com.Payment_Service.demo.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentsModel mockPayment;

    @BeforeEach
    void setUp() {
        mockPayment = PaymentsModel.builder()
                .id(1L)
                .orderId(10L)
                .amount(100)
                .status(StatusPayment.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("registerPayment - Crea pago PENDING exitosamente")
    void testRegisterPayment() {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setOrderId(10L);
        request.setAmount(100);

        when(paymentRepository.save(any(PaymentsModel.class))).thenReturn(mockPayment);

        PaymentResponseDTO response = paymentService.registerPayment(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(StatusPayment.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("getPaymentById - Retorna pago si existe")
    void testGetPaymentByIdSuccess() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        PaymentResponseDTO response = paymentService.getPaymentById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("getPaymentById - Lanza PaymentNotFoundException si no existe")
    void testGetPaymentByIdNotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(99L));
    }

    @Test
    @DisplayName("getPaymentsByOrderId & getAllPayments")
    void testQueries() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of(mockPayment));
        when(paymentRepository.findAll()).thenReturn(List.of(mockPayment));

        assertEquals(1, paymentService.getPaymentsByOrderId(10L).size());
        assertEquals(1, paymentService.getAllPayments().size());
    }

    @Test
    @DisplayName("completePayment - Cambia de PENDING a SUCCESS")
    void testCompletePaymentSuccess() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(PaymentsModel.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponseDTO response = paymentService.completePayment(1L);

        assertEquals(StatusPayment.SUCCESS, response.getStatus());
    }

    @Test
    @DisplayName("completePayment - Excepciones por estados inválidos")
    void testCompletePaymentInvalidState() {
        mockPayment.setStatus(StatusPayment.SUCCESS);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
        assertThrows(InvalidPaymentStateException.class, () -> paymentService.completePayment(1L));

        mockPayment.setStatus(StatusPayment.CANCELLED);
        assertThrows(InvalidPaymentStateException.class, () -> paymentService.completePayment(1L));

        mockPayment.setStatus(StatusPayment.FAILED);
        assertThrows(InvalidPaymentStateException.class, () -> paymentService.completePayment(1L));
    }

    @Test
    @DisplayName("failPayment - Cambia de PENDING a FAILED")
    void testFailPaymentSuccess() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(PaymentsModel.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponseDTO response = paymentService.failPayment(1L);

        assertEquals(StatusPayment.FAILED, response.getStatus());
    }

    @Test
    @DisplayName("failPayment - Excepciones por estados inválidos")
    void testFailPaymentInvalidState() {
        mockPayment.setStatus(StatusPayment.FAILED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
        assertThrows(InvalidPaymentStateException.class, () -> paymentService.failPayment(1L));

        mockPayment.setStatus(StatusPayment.CANCELLED);
        assertThrows(InvalidPaymentStateException.class, () -> paymentService.failPayment(1L));

        mockPayment.setStatus(StatusPayment.SUCCESS);
        assertThrows(InvalidPaymentStateException.class, () -> paymentService.failPayment(1L));
    }

    @Test
    @DisplayName("refundPayment - Reembolsa pago cambiando a CANCELLED")
    void testRefundPaymentSuccess() {
        mockPayment.setStatus(StatusPayment.SUCCESS);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(PaymentsModel.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponseDTO response = paymentService.refundPayment(1L);

        assertEquals(StatusPayment.CANCELLED, response.getStatus());
    }

    @Test
    @DisplayName("refundPayment - Excepciones por estados inválidos")
    void testRefundPaymentInvalidState() {
        mockPayment.setStatus(StatusPayment.CANCELLED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
        assertThrows(InvalidPaymentStateException.class, () -> paymentService.refundPayment(1L));

        mockPayment.setStatus(StatusPayment.FAILED);
        assertThrows(InvalidPaymentStateException.class, () -> paymentService.refundPayment(1L));
    }
}
