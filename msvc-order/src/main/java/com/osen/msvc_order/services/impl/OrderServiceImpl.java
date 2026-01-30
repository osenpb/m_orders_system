package com.osen.msvc_order.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.osen.msvc_order.client.ProductClient;
import com.osen.msvc_order.dtos.ProductDTO;
import com.osen.msvc_order.entities.Order;
import com.osen.msvc_order.entities.OrderItem;
import com.osen.msvc_order.repositories.OrderRepository;
import com.osen.msvc_order.services.OrderItemService;
import com.osen.msvc_order.services.OrderService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final ProductClient productClient;

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }

    @Override
    @CircuitBreaker(name = "productCB", fallbackMethod = "fallBackGetProduct")
    public ProductDTO getProductById(Long productId) {
        return productClient.getProductById(productId);
    }

    // El método fallback DEBE tener la misma firma y recibir la excepción
    private ProductDTO fallBackGetProduct(Long productId, Throwable e) {
        return new ProductDTO(productId, "Producto no disponible (Servicio en mantenimiento)", 0, 0.0);
    }

    @Transactional
    @Override
    public Order createNewOrder(String userId, List<OrderItem> items) {
        Order order = new Order();
        order.setUserId(userId);

        if (items != null) {
            items.forEach(order::addItem);
        }

        return orderRepository.save(order);
    }

}