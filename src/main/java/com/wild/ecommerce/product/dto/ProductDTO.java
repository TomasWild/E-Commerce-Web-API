package com.wild.ecommerce.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDTO(
        UUID id,
        String name,
        String brand,
        String description,
        BigDecimal price,
        int stock,
        String imageUrl,
        String categoryName
) {
}
