package com.wild.ecommerce.category.dto;

import java.util.UUID;

public record CategoryDTO(
        UUID id,
        String name,
        String description
) {
}
