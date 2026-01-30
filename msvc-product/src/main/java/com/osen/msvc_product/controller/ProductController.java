package com.osen.msvc_product.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osen.msvc_product.dtos.CreateProductRequest;
import com.osen.msvc_product.entity.Product;
import com.osen.msvc_product.service.ProductService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        
        List<Product> products = productService.findAll();

        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        
        Optional<Product> optProduct = productService.findById(id);

        if (optProduct.isPresent()) {

            Product product = optProduct.get();

            return ResponseEntity.ok(product);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/my-data")
    public Map<String, Object> getData(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("username", auth.getName());
        
        // Esto mostrará los roles con el prefijo ROLE_ y filtrados
        response.put("authorities", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
                

        return response;
    }


    // @GetMapping("/my-data")
    // public Map<String, Object> getData(@AuthenticationPrincipal Jwt jwt) {
        
    //     Map<String, Object> response = new HashMap<>();
    //     response.put("username", jwt.getClaim("preferred_username"));
    //     response.put("email", jwt.getClaimAsString("email"));

    //     Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    //     if (realmAccess != null) {
    //         response.put("roles", realmAccess.get("roles"));
    //     }
    //     return response;
    // }


    @PostMapping
    public ResponseEntity<?> newProduct(@RequestBody CreateProductRequest entity) {
        Product product = new Product(null,entity.name(), entity.stock(), entity.price());
        
        Product savedProduct = productService.save(product);
        return ResponseEntity.ok(savedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        productService.deleteById(id);
        return ResponseEntity.ok().build();
    }
    

}
