package com.osen.msvc_order.dtos;

import java.util.List;

import jakarta.validation.Valid;

public record UpdateOrderRequest(

    @Valid
    List<CreateOrderItemRequest> items
) {
}
