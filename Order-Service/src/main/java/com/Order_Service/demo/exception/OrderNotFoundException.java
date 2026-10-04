package com.Order_Service.demo.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("No se encontró el pedido con identificador: " + id);
    }

    public OrderNotFoundException(String message) {
        super(message);
    }
}