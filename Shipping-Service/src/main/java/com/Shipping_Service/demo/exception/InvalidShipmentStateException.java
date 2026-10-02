package com.Shipping_Service.demo.exception;

public class InvalidShipmentStateException extends RuntimeException {
    public InvalidShipmentStateException(String message) {
        super(message);
    }
}