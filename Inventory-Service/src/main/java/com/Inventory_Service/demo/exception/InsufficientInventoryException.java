package com.Inventory_Service.demo.exception;

public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(Long productId, int requested, int available) {
        super(String.format(
                "Inventario insuficiente para el producto ID %d: solicitadas %d unidades, disponibles %d unidades",
                productId, requested, available));
    }
}