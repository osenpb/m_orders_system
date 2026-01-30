package com.osen.msvc_order.dtos;

public record ProductDTO(
        Long id,
        String name,
        int stock,
        Double price
) {

}
