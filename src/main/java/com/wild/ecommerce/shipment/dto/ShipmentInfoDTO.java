package com.wild.ecommerce.shipment.dto;

import com.wild.ecommerce.order.model.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShipmentInfoDTO(
        UUID orderId,
        Status status,
        String trackingNumber,
        String carrier,
        LocalDateTime orderDate,
        LocalDateTime shippedDate
) {
}
