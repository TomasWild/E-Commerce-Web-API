package com.wild.ecommerce.cart.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record CartDTO(
        UUID id,
        Set<CartItemDTO> items,
        UUID userId,
        BigDecimal totalPrice,
        int totalItems
) {
}
