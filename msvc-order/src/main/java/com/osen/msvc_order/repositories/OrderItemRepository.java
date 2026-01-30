package com.osen.msvc_order.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.osen.msvc_order.entities.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

}