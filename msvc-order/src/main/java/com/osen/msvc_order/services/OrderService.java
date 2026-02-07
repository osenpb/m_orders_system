package com.osen.msvc_order.services;

import java.util.List;
import java.util.Optional;

import com.osen.msvc_order.dtos.ProductDTO;
import com.osen.msvc_order.entities.Order;
import com.osen.msvc_order.entities.OrderItem;

public interface OrderService {

    List<Order> findAll();

    Optional<Order> findById(Long id);

    Order save(Order order);

    void deleteById(Long id);

    Optional<Order> update(Long id, List<OrderItem> items);

    boolean existsById(Long id);

    ProductDTO getProductById(Long productId);

    Order createNewOrder(String userId, List<OrderItem> items);

}