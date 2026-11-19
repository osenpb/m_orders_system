package com.osen.msvc_product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.osen.msvc_product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Descuento atomico: devuelve 1 si habia stock suficiente y 0 si no.
     * Evita la carrera de leer-comprobar-escribir cuando dos reservas
     * llegan a la vez. Usar dentro de una transaccion.
     */
    @Modifying
    @Query("update Product p set p.stock = p.stock - :quantity where p.id = :id and p.stock >= :quantity")
    int reserveStock(@Param("id") Long id, @Param("quantity") int quantity);
}
