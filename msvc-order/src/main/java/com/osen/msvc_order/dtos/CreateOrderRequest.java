package com.osen.msvc_order.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(

    @NotEmpty(message = "La orden debe tener al menos un item")
    @Valid
    List<CreateOrderItemRequest> items
) {
}
