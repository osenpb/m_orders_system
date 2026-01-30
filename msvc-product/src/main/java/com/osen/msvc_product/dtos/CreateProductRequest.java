package com.osen.msvc_product.dtos;

public record CreateProductRequest(

    String name,
    int stock,
    double price
) {
}
