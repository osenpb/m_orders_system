package com.osen.msvc_order.mappers;

import com.osen.msvc_order.dtos.OrderDTO;
import com.osen.msvc_order.entities.Order;

public class OrderMapper {

    public static OrderDTO toDTO(Order order) {
        return new OrderDTO(
            order.getId(),
            order.getUserId(),
            order.getTotal(),
            OrderItemMapper.toDTO(order.getItems())
        );
    }

}
