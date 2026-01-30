package com.osen.msvc_order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osen.msvc_order.dtos.OrderDTO;
import com.osen.msvc_order.dtos.ProductDTO;
import com.osen.msvc_order.entities.Order;
import com.osen.msvc_order.entities.OrderItem;
import com.osen.msvc_order.mappers.OrderMapper;
import com.osen.msvc_order.services.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    

    @GetMapping("/product/{id}")
    public ResponseEntity<ProductDTO> testProduct(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.getProductById(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable("id") Long id) {
        return ResponseEntity.ok(null);
    }
    

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody List<OrderItem> items, @AuthenticationPrincipal Jwt jwt) {
    
        String userId = jwt.getSubject();
        Order newOrder = orderService.createNewOrder(userId, items);

        OrderDTO newOrderDTO = OrderMapper.toDTO(newOrder);

        return ResponseEntity.accepted().body(newOrderDTO);
    }

    


}
