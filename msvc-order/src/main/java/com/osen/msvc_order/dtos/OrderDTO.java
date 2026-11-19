package com.osen.msvc_order.dtos;

import java.util.List;

import com.osen.msvc_order.entities.OrderStatus;

public record OrderDTO(

        Long id,
        String userId,
        OrderStatus status,
        Double total,
        List<OrderItemDTO> items

) {

}
