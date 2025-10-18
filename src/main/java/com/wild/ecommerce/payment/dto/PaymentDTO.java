package com.wild.ecommerce.payment.dto;

import java.util.UUID;

public record PaymentDTO(
        UUID id,
        String stripePaymentId,
        String stripeName,
        String stripeStatus,
        String stripeResponseMessage,
        String paymentMethod
) {
}
