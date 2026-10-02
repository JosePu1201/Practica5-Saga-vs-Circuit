package com.Shipping_Service.demo.exception;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(Long shipmentId) {
        super("No se encontró el envío con identificador: " + shipmentId);
    }
}