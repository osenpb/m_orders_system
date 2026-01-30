package com.osen.msvc_order.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.osen.msvc_order.entities.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

}