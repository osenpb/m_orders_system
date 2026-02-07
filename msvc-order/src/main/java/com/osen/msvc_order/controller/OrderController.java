package com.osen.msvc_order.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osen.msvc_order.dtos.CreateOrderRequest;
import com.osen.msvc_order.dtos.OrderDTO;
import com.osen.msvc_order.dtos.ProductDTO;
import com.osen.msvc_order.dtos.UpdateOrderRequest;
import com.osen.msvc_order.entities.Order;
import com.osen.msvc_order.entities.OrderItem;
import com.osen.msvc_order.mappers.OrderMapper;
import com.osen.msvc_order.services.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<Order> orders = orderService.findAll();
        List<OrderDTO> orderDTOs = orders.stream()
            .map(OrderMapper::toDTO)
            .toList();
        return ResponseEntity.ok(orderDTOs);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<ProductDTO> testProduct(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.getProductById(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable("id") Long id) {
        Optional<Order> optOrder = orderService.findById(id);
        return optOrder
            .map(order -> ResponseEntity.ok(OrderMapper.toDTO(order)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        List<OrderItem> items = mapToOrderItems(request.items());
        Order newOrder = orderService.createNewOrder(userId, items);
        OrderDTO newOrderDTO = OrderMapper.toDTO(newOrder);

        return ResponseEntity.status(HttpStatus.CREATED).body(newOrderDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> updateOrder(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateOrderRequest request) {

        List<OrderItem> items = mapToOrderItems(request.items());
        Optional<Order> updatedOrder = orderService.update(id, items);

        return updatedOrder
            .map(order -> ResponseEntity.ok(OrderMapper.toDTO(order)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable("id") Long id) {
        if (!orderService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        orderService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private List<OrderItem> mapToOrderItems(List<com.osen.msvc_order.dtos.CreateOrderItemRequest> itemDTOs) {
        if (itemDTOs == null) {
            return List.of();
        }
        return itemDTOs.stream()
            .map(dto -> {
                OrderItem item = new OrderItem();
                item.setProductId(dto.productId());
                item.setQuantity(dto.quantity());
                item.setPrice(dto.price());
                return item;
            })
            .collect(Collectors.toList());
    }
}
