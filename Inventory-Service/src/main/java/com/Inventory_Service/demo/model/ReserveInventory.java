package com.Inventory_Service.demo.model;

import java.time.LocalDateTime;

import com.Inventory_Service.demo.enums.ReserveState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ReserveInventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ReserveState status;
}