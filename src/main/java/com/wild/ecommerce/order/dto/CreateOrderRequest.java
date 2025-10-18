package com.wild.ecommerce.order.dto;

import com.wild.ecommerce.payment.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "Address ID is required")
        UUID addressId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod
) {
}
