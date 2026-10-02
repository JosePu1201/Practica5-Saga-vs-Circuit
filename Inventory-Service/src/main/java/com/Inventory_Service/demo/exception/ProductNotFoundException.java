package com.Inventory_Service.demo.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long productId) {
        super("No se encontró el registro de inventario para el producto con ID: " + productId);
    }
}