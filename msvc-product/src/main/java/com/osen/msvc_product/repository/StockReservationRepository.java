package com.osen.msvc_product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.osen.msvc_product.entity.StockReservation;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}
