package com.osen.msvc_product.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import com.osen.msvc_product.entity.Product;
import com.osen.msvc_product.repository.ProductRepository;

@Configuration
public class DataInit implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataInit(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if(productRepository.findAll().isEmpty()) {
            Product product1 = new Product();
            product1.setName("Producto 1");
            product1.setStock(10);
            product1.setPrice(100.0);
            productRepository.save(product1);

            Product product2 = new Product();
            product2.setName("Producto 2");
            product2.setStock(20);
            product2.setPrice(200.0);
            productRepository.save(product2);

            Product product3 = new Product();
            product3.setName("Producto 3");
            product3.setStock(30);
            product3.setPrice(300.0);
            productRepository.save(product3);
        }

    }

}
