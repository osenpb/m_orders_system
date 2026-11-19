package com.osen.msvc_order.exceptions;

public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(Throwable cause) {
        super("El servicio de productos no esta disponible", cause);
    }
}
