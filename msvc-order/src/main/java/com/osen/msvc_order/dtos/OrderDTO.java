package com.osen.msvc_order.dtos;

import java.util.List;

public record OrderDTO(

        Long id,
        String userId,
        Double total,
        List<OrderItemDTO> items

) {

}
