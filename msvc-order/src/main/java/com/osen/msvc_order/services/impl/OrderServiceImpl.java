package com.osen.msvc_order.services.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.osen.msvc_order.client.ProductGateway;
import com.osen.msvc_order.dtos.ProductDTO;
import com.osen.msvc_order.entities.Order;
import com.osen.msvc_order.entities.OrderItem;
import com.osen.msvc_order.repositories.OrderRepository;
import com.osen.msvc_order.services.OrderItemService;
import com.osen.msvc_order.services.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final ProductGateway productGateway;
    private final TransactionTemplate tx;

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemService orderItemService,
            ProductGateway productGateway, PlatformTransactionManager transactionManager) {
        this.orderRepository = orderRepository;
        this.orderItemService = orderItemService;
        this.productGateway = productGateway;
        this.tx = new TransactionTemplate(transactionManager);
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
    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }

    @Override
    public Optional<Order> update(Long id, List<OrderItem> items) {
        // Los precios se consultan fuera de la transaccion: no se retiene una
        // conexion de BD mientras se espera la respuesta de msvc-product.
        priceItems(items);
        return tx.execute(status -> {
            Optional<Order> optOrder = orderRepository.findById(id);
            if (optOrder.isPresent()) {
                Order order = optOrder.get();
                order.getItems().clear();
                if (items != null) {
                    items.forEach(order::addItem);
                }
                return Optional.of(orderRepository.save(order));
            }
            return Optional.<Order>empty();
        });
    }

    @Override
    public boolean existsById(Long id) {
        return orderRepository.existsById(id);
    }

    @Override
    public ProductDTO getProductById(Long productId) {
        return productGateway.getProductById(productId);
    }

    @Override
    public Order createNewOrder(String userId, List<OrderItem> items) {
        priceItems(items);
        return tx.execute(status -> {
            Order order = new Order();
            order.setUserId(userId);
            if (items != null) {
                items.forEach(order::addItem);
            }
            return orderRepository.save(order);
        });
    }

    /**
     * Valida que cada producto exista y toma el precio de msvc-product, nunca
     * del cliente. El stock NO se valida aqui: lo decide la SAGA.
     */
    private void priceItems(List<OrderItem> items) {
        if (items == null) {
            return;
        }
        for (OrderItem item : items) {
            ProductDTO product = productGateway.getProductById(item.getProductId());
            item.setPrice(product.price());
        }
    }

}
