package com.wild.ecommerce.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @NotBlank(message = "Country is required")
        @Size(min = 2, max = 50, message = "Country must be between 2 and 50 characters")
        String country,

        @NotBlank(message = "State or Province is required")
        @Size(min = 2, max = 100, message = "State or Province must be between 2 and 100 characters")
        String state,

        @NotBlank(message = "City is required")
        @Size(min = 2, max = 200, message = "City must be between 2 and 200 characters")
        String city,

        @NotBlank(message = "Street is required")
        @Size(min = 1, max = 500, message = "Street must be between 1 and 500 characters")
        String street,

        @NotBlank(message = "Postal Code is required")
        @Size(min = 3, max = 20, message = "Postal code must be between 3 and 20 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9\\s-]+$",
                message = "Postal code can only contain letters, numbers, spaces, and hyphens"
        )
        String postalCode
) {
}
