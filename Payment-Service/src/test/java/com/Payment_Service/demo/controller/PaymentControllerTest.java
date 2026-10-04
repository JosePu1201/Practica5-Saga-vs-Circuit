package com.Payment_Service.demo.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.Payment_Service.demo.dto.PaymentRequestDTO;
import com.Payment_Service.demo.dto.PaymentResponseDTO;
import com.Payment_Service.demo.enums.StatusPayment;
import com.Payment_Service.demo.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private PaymentResponseDTO mockPaymentDTO;

    @BeforeEach
    void setUp() {
        mockPaymentDTO = new PaymentResponseDTO();
        mockPaymentDTO.setId(1L);
        mockPaymentDTO.setStatus(StatusPayment.PENDING);
    }

    @Test
    @DisplayName("registerPayment - Retorna HTTP 201 CREATED")
    void testRegisterPayment() {
        when(paymentService.registerPayment(any())).thenReturn(mockPaymentDTO);
        ResponseEntity<PaymentResponseDTO> response = paymentController.registerPayment(new PaymentRequestDTO());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("getPaymentById, getPaymentsByOrderId, getAllPayments - Retorna HTTP 200 OK")
    void testGetQueries() {
        when(paymentService.getPaymentById(1L)).thenReturn(mockPaymentDTO);
        when(paymentService.getPaymentsByOrderId(10L)).thenReturn(List.of(mockPaymentDTO));
        when(paymentService.getAllPayments()).thenReturn(List.of(mockPaymentDTO));

        assertEquals(HttpStatus.OK, paymentController.getPaymentById(1L).getStatusCode());
        assertEquals(HttpStatus.OK, paymentController.getPaymentsByOrderId(10L).getStatusCode());
        assertEquals(HttpStatus.OK, paymentController.getAllPayments().getStatusCode());
    }

    @Test
    @DisplayName("completePayment, failPayment, refundPayment - Retorna HTTP 200 OK")
    void testStatusTransitions() {
        when(paymentService.completePayment(1L)).thenReturn(mockPaymentDTO);
        when(paymentService.failPayment(1L)).thenReturn(mockPaymentDTO);
        when(paymentService.refundPayment(1L)).thenReturn(mockPaymentDTO);

        assertEquals(HttpStatus.OK, paymentController.completePayment(1L).getStatusCode());
        assertEquals(HttpStatus.OK, paymentController.failPayment(1L).getStatusCode());
        assertEquals(HttpStatus.OK, paymentController.refundPayment(1L).getStatusCode());
    }
}
