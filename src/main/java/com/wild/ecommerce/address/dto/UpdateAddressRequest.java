package com.wild.ecommerce.address.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @Size(min = 2, max = 50, message = "Country must be between 2 and 50 characters")
        String country,

        @Size(min = 2, max = 100, message = "State or Province must be between 2 and 100 characters")
        String state,

        @Size(min = 2, max = 200, message = "City must be between 2 and 200 characters")
        String city,

        @Size(min = 1, max = 500, message = "Street must be between 1 and 500 characters")
        String street,

        @Size(min = 3, max = 20, message = "Postal code must be between 3 and 20 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9\\s-]+$",
                message = "Postal code can only contain letters, numbers, spaces, and hyphens"
        )
        String postalCode
) {
}
