package com.osen.msvc_order.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(

    @NotNull(message = "El ID del producto es obligatorio")
    Long productId,

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    Integer quantity,

    @NotNull(message = "El precio es obligatorio")
    Double price
) {
}
