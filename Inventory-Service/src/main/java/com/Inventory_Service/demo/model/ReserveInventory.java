package com.Inventory_Service.demo.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "ReserveInventory")
@Data
public class ReserveInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reserve_id")
    private Long reserveId;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private InventoryModel inventoryModel;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "dateReserved")
    private LocalDateTime dateReserved;
}
