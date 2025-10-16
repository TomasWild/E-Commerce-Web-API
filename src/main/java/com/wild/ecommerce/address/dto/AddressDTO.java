package com.wild.ecommerce.address.dto;

import java.util.UUID;

public record AddressDTO(
        UUID id,
        String country,
        String state,
        String city,
        String street,
        String postalCode
) {
}
