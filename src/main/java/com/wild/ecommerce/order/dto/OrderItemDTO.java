package com.wild.ecommerce.order.dto;

import com.wild.ecommerce.product.dto.ProductDTO;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDTO(
        UUID id,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        ProductDTO product
) {
}
