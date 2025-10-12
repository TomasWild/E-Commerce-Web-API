package com.wild.ecommerce.authentication.dto;

import com.wild.ecommerce.authentication.TokenType;

public record AuthenticationResponse(
        String email,
        String role,
        String accessToken,
        String refreshToken,
        TokenType tokenType
) {
}
