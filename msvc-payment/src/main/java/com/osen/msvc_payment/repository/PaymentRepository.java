package com.osen.msvc_payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.osen.msvc_payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
