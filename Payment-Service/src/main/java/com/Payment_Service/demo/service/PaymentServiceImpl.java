package com.Payment_Service.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Payment_Service.demo.dto.PaymentRequestDTO;
import com.Payment_Service.demo.dto.PaymentResponseDTO;
import com.Payment_Service.demo.enums.StatusPayment;
import com.Payment_Service.demo.exception.InvalidPaymentStateException;
import com.Payment_Service.demo.exception.PaymentNotFoundException;
import com.Payment_Service.demo.model.PaymentsModel;
import com.Payment_Service.demo.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PaymentResponseDTO registerPayment(PaymentRequestDTO requestDto) {
        LocalDateTime now = LocalDateTime.now();

        PaymentsModel payment = PaymentsModel.builder()
                .orderId(requestDto.getOrderId())
                .amount(requestDto.getAmount())
                .status(StatusPayment.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PaymentsModel saved = paymentRepository.save(payment);
        return PaymentResponseDTO.fromEntity(saved, "Pago registrado en estado pendiente de cobro (simulación).");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(Long id) {
        PaymentsModel payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return PaymentResponseDTO.fromEntity(payment, "Consulta de pago exitosa.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(p -> PaymentResponseDTO.fromEntity(p, "Pago asociado a la orden " + orderId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(p -> PaymentResponseDTO.fromEntity(p, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentResponseDTO completePayment(Long id) {
        PaymentsModel payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() == StatusPayment.SUCCESS) {
            throw new InvalidPaymentStateException("El pago ya se encuentra en estado completado (SUCCESS)");
        }
        if (payment.getStatus() == StatusPayment.CANCELLED) {
            throw new InvalidPaymentStateException("No se puede completar un pago que ha sido cancelado o reembolsado");
        }
        if (payment.getStatus() == StatusPayment.FAILED) {
            throw new InvalidPaymentStateException("No se puede completar un pago previamente fallido");
        }

        payment.setStatus(StatusPayment.SUCCESS);
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentsModel updated = paymentRepository.save(payment);
        return PaymentResponseDTO.fromEntity(updated, "Cobro confirmado exitosamente (simulado).");
    }

    @Override
    @Transactional
    public PaymentResponseDTO failPayment(Long id) {
        PaymentsModel payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() == StatusPayment.FAILED) {
            throw new InvalidPaymentStateException("El pago ya se encuentra marcado como fallido (FAILED)");
        }
        if (payment.getStatus() == StatusPayment.CANCELLED) {
            throw new InvalidPaymentStateException("No se puede marcar como fallido un pago que fue reembolsado");
        }
        if (payment.getStatus() == StatusPayment.SUCCESS) {
            throw new InvalidPaymentStateException(
                    "No se puede marcar directamente como fallido un pago que ya fue completado; use reembolso");
        }

        payment.setStatus(StatusPayment.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentsModel updated = paymentRepository.save(payment);
        return PaymentResponseDTO.fromEntity(updated, "Transacción de cobro simulada marcada como fallida.");
    }

    @Override
    @Transactional
    public PaymentResponseDTO refundPayment(Long id) {
        PaymentsModel payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() == StatusPayment.CANCELLED) {
            throw new InvalidPaymentStateException("El pago con ID " + id + " ya ha sido reembolsado previamente");
        }
        if (payment.getStatus() == StatusPayment.FAILED) {
            throw new InvalidPaymentStateException("No se puede reembolsar un pago que falló en su cobro original");
        }

        // Transición compensatoria de la Saga hacia CANCELLED
        payment.setStatus(StatusPayment.CANCELLED);
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentsModel refunded = paymentRepository.save(payment);
        return PaymentResponseDTO.fromEntity(refunded,
                "Reembolso procesado exitosamente (compensación Saga simulada).");
    }
}