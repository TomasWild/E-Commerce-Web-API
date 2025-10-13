package com.wild.ecommerce.auth.service;

import com.wild.ecommerce.auth.dto.AuthenticationRequest;
import com.wild.ecommerce.auth.dto.AuthenticationResponse;
import com.wild.ecommerce.auth.dto.RegisterRequest;
import com.wild.ecommerce.auth.model.TokenType;
import com.wild.ecommerce.auth.model.VerificationToken;
import com.wild.ecommerce.auth.repository.VerificationTokenRepository;
import com.wild.ecommerce.common.exception.AccountAlreadyVerifiedException;
import com.wild.ecommerce.common.exception.InvalidTokenException;
import com.wild.ecommerce.common.exception.ResourceAlreadyExistsException;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.notification.service.EmailService;
import com.wild.ecommerce.user.model.Role;
import com.wild.ecommerce.user.model.User;
import com.wild.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private RegisterRequest registerRequest;
    private AuthenticationRequest authenticationRequest;
    private User user;
    private VerificationToken verificationToken;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();

        registerRequest = new RegisterRequest(
                "User",
                "Test",
                "user.test@example.com",
                "Test@password123",
                "Test@password123"
        );

        authenticationRequest = new AuthenticationRequest(
                "user.test@example.com",
                "Test@password123"
        );

        user = new User();
        user.setId(id);
        user.setFirstName("User");
        user.setLastName("Test");
        user.setEmail("user.test@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);
        user.setEnabled(false);

        verificationToken = new VerificationToken();
        verificationToken.setId(id);
        verificationToken.setToken("test-token");
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        verificationToken.setUsed(false);
    }

    @Test
    void register_Success() {
        // Given
        when(userRepository.findByEmailIgnoreCase(registerRequest.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);

        // When
        String result = authenticationService.register(registerRequest);

        // Then
        assertThat(result).isEqualTo("Registration successful. Please check your email to verify your account");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getFirstName()).isEqualTo("User");
        assertThat(savedUser.getLastName()).isEqualTo("Test");
        assertThat(savedUser.getEmail()).isEqualTo("user.test@example.com");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.isEnabled()).isFalse();

        verify(emailService).sendVerificationEmail(eq("User"), eq("user.test@example.com"), anyString());
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        // Given
        when(userRepository.findByEmailIgnoreCase(registerRequest.email())).thenReturn(Optional.of(user));

        // // When & Then
        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("User with email 'user.test@example.com' already exists");

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void authenticate_Success() {
        // Given
        user.setEnabled(true);
        when(userRepository.findByEmailIgnoreCase(authenticationRequest.email())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        // When
        AuthenticationResponse response = authenticationService.authenticate(authenticationRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("user.test@example.com");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo(TokenType.BEARER);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticate_UserDisabled_ThrowsException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User account is disabled"));

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(authenticationRequest))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("User with email 'user.test@example.com' is disabled");
    }

    @Test
    void authenticate_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByEmailIgnoreCase(authenticationRequest.email())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(authenticationRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with email 'user.test@example.com' not found");
    }

    @Test
    void verifyEmail_Success() {
        // Given
        when(verificationTokenRepository.findByToken("test-token")).thenReturn(Optional.of(verificationToken));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);

        // When
        String result = authenticationService.verifyEmail("test-token");

        // Then
        assertThat(result).isEqualTo("Email verified successfully. You can now login");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isEnabled()).isTrue();

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);

        verify(verificationTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().isUsed()).isTrue();
    }

    @Test
    void verifyEmail_TokenNotFound_ThrowsException() {
        // Given
        when(verificationTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authenticationService.verifyEmail("invalid-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Verification token not found");
    }

    @Test
    void verifyEmail_TokenAlreadyUsed_ThrowsException() {
        // Given
        verificationToken.setUsed(true);
        when(verificationTokenRepository.findByToken("test-token")).thenReturn(Optional.of(verificationToken));

        // When & Then
        assertThatThrownBy(() -> authenticationService.verifyEmail("test-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token already used");
    }

    @Test
    void verifyEmail_TokenExpired_ThrowsException() {
        // Given
        verificationToken.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        when(verificationTokenRepository.findByToken("test-token")).thenReturn(Optional.of(verificationToken));

        // When & Then
        assertThatThrownBy(() -> authenticationService.verifyEmail("test-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token expired. Please request a new verification email");
    }

    @Test
    void resendVerificationEmail_Success() {
        // Given
        when(userRepository.findByEmailIgnoreCase("user.test@example.com")).thenReturn(Optional.of(user));
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);

        // When
        String result = authenticationService.resendVerificationEmail("user.test@example.com");

        // Then
        assertThat(result).isEqualTo("Verification email sent. Please check your inbox");
        verify(emailService).sendVerificationEmail(eq("User"), eq("user.test@example.com"), anyString());
        verify(verificationTokenRepository).save(any(VerificationToken.class));
    }

    @Test
    void resendVerificationEmail_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authenticationService.resendVerificationEmail("nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with email 'nonexistent@example.com' not found");
    }

    @Test
    void resendVerificationEmail_AccountAlreadyVerified_ThrowsException() {
        // Given
        user.setEnabled(true);
        when(userRepository.findByEmailIgnoreCase("user.test@example.com")).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> authenticationService.resendVerificationEmail("user.test@example.com"))
                .isInstanceOf(AccountAlreadyVerifiedException.class)
                .hasMessageContaining("Account already verified");
    }
}
