package com.osen.msvc_order.mappers;

import java.util.List;

import com.osen.msvc_order.dtos.OrderItemDTO;
import com.osen.msvc_order.entities.OrderItem;

public class OrderItemMapper {

    public static OrderItemDTO toDTO(OrderItem orderItem) {

        return new OrderItemDTO(
            orderItem.getId(),
            orderItem.getProductId(),
            orderItem.getQuantity(),
            orderItem.getPrice()
        );

    }

    public static List<OrderItemDTO> toDTO(List<OrderItem> orderItems) {
        return orderItems.stream()
            .map(OrderItemMapper::toDTO)
            .toList();
    }


}
