package com.Order_Service.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Order_Service.demo.model.OrdersModel;

@Repository
public interface OrdersRepository extends JpaRepository<OrdersModel, Long> {
}
