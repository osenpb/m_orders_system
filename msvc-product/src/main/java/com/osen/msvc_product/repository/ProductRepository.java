package com.osen.msvc_product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.osen.msvc_product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> { }
