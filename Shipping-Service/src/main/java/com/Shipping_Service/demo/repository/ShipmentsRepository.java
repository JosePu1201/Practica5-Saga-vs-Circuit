package com.Shipping_Service.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Shipping_Service.demo.model.ShipmentsModel;

@Repository
public interface ShipmentsRepository extends JpaRepository<ShipmentsModel, Long> {
    List<ShipmentsModel> findByOrderId(Long orderId);
}