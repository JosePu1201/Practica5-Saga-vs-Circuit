package com.Inventory_Service.demo.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "Inventory")
@Data
public class InventoryModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;
    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "dateUpdated")
    private LocalDateTime dateUpdated;

    @Column(name = "reserved_stock", columnDefinition = "integer default 0")
    private Integer reservedStock;
}
