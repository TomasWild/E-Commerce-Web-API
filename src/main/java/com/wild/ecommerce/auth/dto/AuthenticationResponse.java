package com.wild.ecommerce.auth.dto;

import com.wild.ecommerce.auth.model.TokenType;

public record AuthenticationResponse(
        String email,
        String role,
        String accessToken,
        String refreshToken,
        TokenType tokenType
) {
}
