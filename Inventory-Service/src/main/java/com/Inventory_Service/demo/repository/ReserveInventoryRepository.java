package com.Inventory_Service.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Inventory_Service.demo.model.ReserveInventory;

@Repository
public interface ReserveInventoryRepository extends JpaRepository<ReserveInventory, Long> {
    List<ReserveInventory> findByInventoryModelProductId(Long productId);
}