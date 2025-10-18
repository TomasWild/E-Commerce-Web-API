package com.wild.ecommerce.order.dto;

import com.wild.ecommerce.order.model.Status;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateOrderRequest(
        @NotNull(message = "Status is required")
        Status status,
        UUID addressId
) {
}
