package com.wild.ecommerce.shipment.dto;

import jakarta.validation.constraints.NotBlank;

public record ShipOrderRequest(
        @NotBlank(message = "Carrier is required")
        String carrier,
        String trackingNumber
) {
}
