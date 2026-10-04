package com.Inventory_Service.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Inventory_Service.demo.model.InventoryModel;

import jakarta.persistence.LockModeType;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryModel, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryModel i WHERE i.productId = :id")
    Optional<InventoryModel> findByIdForUpdate(@Param("id") Long id);
}