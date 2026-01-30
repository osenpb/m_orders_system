package com.osen.msvc_product.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.osen.msvc_product.entity.Product;
import com.osen.msvc_product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    
    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public boolean update(Product product) {
        if (productRepository.existsById(product.getId())) {
            productRepository.save(product);
            return true;
        }
        return false;
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return productRepository.findById(productId);
    }

    @Override
    public void deleteById(Long productId) {
        productRepository.deleteById(productId);
    }
}

