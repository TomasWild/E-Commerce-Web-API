package com.wild.ecommerce.authentication.dto;

import com.wild.ecommerce.common.util.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordMatches
public record RegisterRequest(
        @NotBlank(message = "First name is required")
        @Size(min = 3, max = 50, message = "First name must be between 3 and 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 3, max = 50, message = "Last name must be between 3 and 50 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
        )
        String password,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword
) {
}
