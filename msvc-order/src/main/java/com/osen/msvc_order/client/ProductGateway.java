package com.osen.msvc_order.client;

import org.springframework.stereotype.Component;

import com.osen.msvc_order.dtos.ProductDTO;
import com.osen.msvc_order.exceptions.ProductNotFoundException;
import com.osen.msvc_order.exceptions.ProductServiceUnavailableException;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * Unico punto que llama a msvc-product. Vive en su propio bean para que el
 * circuit breaker pase por el proxy de Spring (una llamada interna dentro de
 * OrderServiceImpl lo saltaria). No hay fallback con datos inventados: si el
 * producto no se puede consultar, la llamada falla.
 */
@Component
public class ProductGateway {

    private final ProductClient productClient;

    public ProductGateway(ProductClient productClient) {
        this.productClient = productClient;
    }

    @CircuitBreaker(name = "productCB")
    public ProductDTO getProductById(Long productId) {
        try {
            return productClient.getProductById(productId);
        } catch (FeignException.NotFound e) {
            throw new ProductNotFoundException(productId);
        } catch (FeignException e) {
            throw new ProductServiceUnavailableException(e);
        }
    }
}
