package com.Inventory_Service.demo.exception;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Long reserveId) {
        super("No se encontró la reserva con ID: " + reserveId);
    }
}