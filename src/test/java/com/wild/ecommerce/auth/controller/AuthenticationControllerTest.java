package com.wild.ecommerce.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wild.ecommerce.auth.dto.AuthenticationRequest;
import com.wild.ecommerce.auth.dto.AuthenticationResponse;
import com.wild.ecommerce.auth.dto.RegisterRequest;
import com.wild.ecommerce.auth.model.TokenType;
import com.wild.ecommerce.auth.service.AuthenticationService;
import com.wild.ecommerce.auth.service.JwtService;
import com.wild.ecommerce.user.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void register_WithValidRequest_ReturnsCreatedStatus() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "User",
                "Test",
                "user.test@example.com",
                "Test@password123",
                "Test@password123"
        );

        String expectedResponse = "Registration successful. Please check your email to verify your account.";
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string(expectedResponse));
    }

    @Test
    void register_WithInvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        String invalidRequest = "{}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithValidCredentials_ReturnsOkAndToken() throws Exception {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest(
                "user.test@example.com",
                "Test@password123"
        );

        AuthenticationResponse response = new AuthenticationResponse(
                "user.test@example.com",
                Role.USER.name(),
                "jwt-token-here",
                "refresh-token-here",
                TokenType.BEARER
        );

        when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user.test@example.com"))
                .andExpect(jsonPath("$.role").value(Role.USER.name()))
                .andExpect(jsonPath("$.accessToken").value("jwt-token-here"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-here"))
                .andExpect(jsonPath("$.tokenType").value(TokenType.BEARER.toString()));
    }

    @Test
    void login_WithInvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest(
                "user.test@example.com",
                "Test@password123"
        );

        when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void verifyEmail_WithValidToken_ReturnsOk() throws Exception {
        // Arrange
        String token = "valid-verification-token";
        String expectedResponse = "Email verified successfully";

        when(authenticationService.verifyEmail(eq(token)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/verify").param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResponse));
    }

    @Test
    void verifyEmail_WithInvalidToken_ReturnsError() throws Exception {
        // Arrange
        String token = "invalid-token";

        when(authenticationService.verifyEmail(eq(token)))
                .thenThrow(new RuntimeException("Invalid or expired token"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/verify").param("token", token))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void verifyEmail_WithMissingToken_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/verify"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendVerificationEmail_WithValidEmail_ReturnsOk() throws Exception {
        // Arrange
        String email = "test@example.com";
        String expectedResponse = "Verification email sent successfully";

        when(authenticationService.resendVerificationEmail(eq(email)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/resend-verification").param("email", email))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResponse));
    }

    @Test
    void resendVerificationEmail_WithNonexistentEmail_ReturnsError() throws Exception {
        // Arrange
        String email = "nonexistent@example.com";

        when(authenticationService.resendVerificationEmail(eq(email)))
                .thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/resend-verification").param("email", email))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void resendVerificationEmail_WithMissingEmail_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/resend-verification"))
                .andExpect(status().isBadRequest());
    }
}
