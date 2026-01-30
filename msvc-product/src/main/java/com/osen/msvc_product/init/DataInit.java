package com.osen.msvc_product.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import com.osen.msvc_product.entity.Product;
import com.osen.msvc_product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        if(productRepository.findAll().isEmpty()) {
            productRepository.save(new Product(null, "Producto 1", 10, 100.0));
            productRepository.save(new Product(null ,"Producto 2", 20, 200.0));
            productRepository.save(new Product(null ,"Producto 3", 30, 300.0));
        }    
    
    }
    
}
