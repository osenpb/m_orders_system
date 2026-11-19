package com.osen.msvc_order.exceptions;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("El producto " + productId + " no existe");
    }
}
