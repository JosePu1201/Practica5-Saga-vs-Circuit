package com.Payment_Service.demo.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(Long id) {
        super("No se encontró el pago con identificador: " + id);
    }

    public PaymentNotFoundException(String message) {
        super(message);
    }
}