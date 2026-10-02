package com.Payment_Service.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Payment_Service.demo.model.PaymentsModel;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentsModel, Long> {
}
