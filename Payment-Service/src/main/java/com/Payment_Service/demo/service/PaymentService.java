package com.Payment_Service.demo.service;

import java.util.List;

import com.Payment_Service.demo.dto.PaymentRequestDTO;
import com.Payment_Service.demo.dto.PaymentResponseDTO;

public interface PaymentService {
    PaymentResponseDTO registerPayment(PaymentRequestDTO requestDto);

    PaymentResponseDTO getPaymentById(Long id);

    List<PaymentResponseDTO> getPaymentsByOrderId(Long orderId);

    List<PaymentResponseDTO> getAllPayments();

    PaymentResponseDTO completePayment(Long id);

    PaymentResponseDTO failPayment(Long id);

    PaymentResponseDTO refundPayment(Long id);
}