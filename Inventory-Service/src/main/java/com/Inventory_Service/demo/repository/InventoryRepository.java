package com.Inventory_Service.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Inventory_Service.demo.model.InventoryModel;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryModel, Long> {
}
