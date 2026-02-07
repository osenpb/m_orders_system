package com.osen.msvc_order.services.impl;

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

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final ProductClient productClient;

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemService orderItemService, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.orderItemService = orderItemService;
        this.productClient = productClient;
    }

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
    @Transactional
    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Optional<Order> update(Long id, List<OrderItem> items) {
        Optional<Order> optOrder = orderRepository.findById(id);
        if (optOrder.isPresent()) {
            Order order = optOrder.get();
            order.getItems().clear();
            if (items != null) {
                items.forEach(order::addItem);
            }
            return Optional.of(orderRepository.save(order));
        }
        return Optional.empty();
    }

    @Override
    public boolean existsById(Long id) {
        return orderRepository.existsById(id);
    }

    @Override
    @CircuitBreaker(name = "productCB", fallbackMethod = "fallBackGetProduct")
    public ProductDTO getProductById(Long productId) {
        return productClient.getProductById(productId);
    }

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
