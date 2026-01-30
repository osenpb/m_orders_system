package com.osen.msvc_product.service;

import java.util.List;
import java.util.Optional;

import com.osen.msvc_product.entity.Product;

public interface ProductService {

    List<Product> findAll();
    Product save(Product product);
    boolean update(Product product);
    Optional<Product> findById(Long productId);
    void deleteById(Long productId);

}
