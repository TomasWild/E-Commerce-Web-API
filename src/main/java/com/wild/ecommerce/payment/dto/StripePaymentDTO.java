package com.wild.ecommerce.payment.dto;

import com.wild.ecommerce.address.dto.AddressDTO;

import java.util.Map;

public record StripePaymentDTO(
        Long amountInCents,
        String currency,
        String name,
        String email,
        String description,
        AddressDTO addressDTO,
        Map<String, String> metadata
) {
}
