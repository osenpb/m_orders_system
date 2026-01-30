package com.osen.msvc_order.services;

import java.util.List;
import java.util.Optional;

import com.osen.msvc_order.entities.OrderItem;

public interface OrderItemService {

    List<OrderItem> findAll();

    Optional<OrderItem> findById(Long id);

    OrderItem save(OrderItem orderItem);

    void deleteById(Long id);

}