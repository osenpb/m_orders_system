package com.osen.msvc_order.dtos;

public record OrderItemDTO(
        Long id,
        Long productId,
        Integer quantity,
        Double price
) {

}
