package com.wild.ecommerce.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDTO(
        UUID id,
        UUID productId,
        String productName,
        BigDecimal price,
        int quantity,
        BigDecimal totalPrice
) {
}
