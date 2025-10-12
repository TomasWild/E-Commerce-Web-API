package com.wild.ecommerce.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wild.ecommerce.authentication.JwtService;
import com.wild.ecommerce.common.exception.InvalidPasswordException;
import com.wild.ecommerce.user.dto.ChangePasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    public void setUp() {
        changePasswordRequest = new ChangePasswordRequest(
                "current@Password123",
                "new@Password456",
                "new@Password456"
        );
    }

    @Test
    void changePassword_ValidRequest_ReturnsOk() throws Exception {
        // Given
        doNothing().when(userService).changePassword(eq(changePasswordRequest), any());

        // When & Then
        mockMvc.perform(patch("/api/v1/users")
                        .with(user("test.user@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk());

        verify(userService).changePassword(eq(changePasswordRequest), any());
    }

    @Test
    void changePassword_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        final ChangePasswordRequest invalidRequest = new ChangePasswordRequest(
                "",
                "NewPassword123!",
                "NewPassword123!"
        );

        // When & Then
        mockMvc.perform(patch("/api/v1/users")
                        .with(user("test.user@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(userService, org.mockito.Mockito.never()).changePassword(any(), any());
    }

    @Test
    void changePassword_ServiceThrowsUsernameNotFoundException_ReturnsNotFound() throws Exception {
        // Given
        doThrow(new UsernameNotFoundException("User not found")).when(userService)
                .changePassword(eq(changePasswordRequest), any());

        // When & Then
        mockMvc.perform(patch("/api/v1/users")
                        .with(user("nonexistent@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isNotFound());

        verify(userService).changePassword(eq(changePasswordRequest), any());
    }

    @Test
    void changePassword_ServiceThrowsInvalidPasswordException_ReturnsBadRequest() throws Exception {
        // Given
        doThrow(new InvalidPasswordException("Wrong password"))
                .when(userService).changePassword(eq(changePasswordRequest), any());

        // When & Then
        mockMvc.perform(patch("/api/v1/users")
                        .with(user("test.user@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest());

        verify(userService).changePassword(eq(changePasswordRequest), any());
    }
}
