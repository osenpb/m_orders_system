package com.osen.msvc_order.services.impl;

import java.util.List;
import java.util.Optional;


import org.springframework.stereotype.Service;

import com.osen.msvc_order.entities.OrderItem;
import com.osen.msvc_order.repositories.OrderItemRepository;
import com.osen.msvc_order.services.OrderItemService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;

    @Override
    public List<OrderItem> findAll() {
        return orderItemRepository.findAll();
    }

    @Override
    public Optional<OrderItem> findById(Long id) {
        return orderItemRepository.findById(id);
    }

    @Override
    public OrderItem save(OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }

    @Override
    public void deleteById(Long id) {
        orderItemRepository.deleteById(id);
    }



}