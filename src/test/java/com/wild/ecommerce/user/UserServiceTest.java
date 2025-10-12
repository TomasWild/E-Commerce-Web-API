package com.wild.ecommerce.user;

import com.wild.ecommerce.common.exception.InvalidPasswordException;
import com.wild.ecommerce.user.dto.ChangePasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Principal principal;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private ChangePasswordRequest request;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();

        user = new User();
        user.setId(id);
        user.setEmail("test.user@example.com");
        user.setPassword("encodedCurrentPassword");

        request = new ChangePasswordRequest(
                "CurrentPassword123!",
                "NewPassword123!",
                "NewPassword123!"
        );
    }

    @Test
    void shouldChangePassword_WhenCurrentPasswordIsValidAndNewPasswordIsDifferent() {
        // Given
        when(principal.getName()).thenReturn("john.doe@example.com");
        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPassword123!", "encodedCurrentPassword"))
                .thenReturn(true);
        when(passwordEncoder.matches("NewPassword123!", "encodedCurrentPassword"))
                .thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.changePassword(request, principal);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPassword()).isEqualTo("encodedNewPassword");
        verify(passwordEncoder).encode("NewPassword123!");
    }

    @Test
    void shouldThrowUsernameNotFoundException_WhenUserDoesNotExist() {
        // Given
        when(principal.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(request, principal))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldThrowInvalidPasswordException_WhenCurrentPasswordIsIncorrect() {
        // Given
        when(principal.getName()).thenReturn("john.doe@example.com");
        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPassword123!", "encodedCurrentPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(request, principal))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Wrong password");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldThrowInvalidPasswordException_WhenNewPasswordIsSameAsCurrent() {
        // Given
        ChangePasswordRequest samePasswordRequest = new ChangePasswordRequest(
                "CurrentPassword123!",
                "CurrentPassword123!",
                "CurrentPassword123!"
        );

        when(principal.getName()).thenReturn("john.doe@example.com");
        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPassword123!", "encodedCurrentPassword"))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(samePasswordRequest, principal))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("New password must be different from current password");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldThrowInvalidPasswordException_WhenNewPasswordAndConfirmPasswordDoNotMatch() {
        // Given
        ChangePasswordRequest mismatchRequest = new ChangePasswordRequest(
                "CurrentPassword123!",
                "NewPassword123!",
                "DifferentPassword123!"
        );

        when(principal.getName()).thenReturn("john.doe@example.com");
        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPassword123!", "encodedCurrentPassword"))
                .thenReturn(true);
        when(passwordEncoder.matches("NewPassword123!", "encodedCurrentPassword"))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(mismatchRequest, principal))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Passwords do not match");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldValidatePasswordsInCorrectOrder_WhenChangingPassword() {
        // Given
        when(principal.getName()).thenReturn("john.doe@example.com");
        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPassword123!", "encodedCurrentPassword"))
                .thenReturn(true);
        when(passwordEncoder.matches("NewPassword123!", "encodedCurrentPassword"))
                .thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.changePassword(request, principal);

        // Then
        InOrder inOrder = inOrder(passwordEncoder, userRepository);

        inOrder.verify(passwordEncoder).matches("CurrentPassword123!", "encodedCurrentPassword");
        inOrder.verify(passwordEncoder).matches("NewPassword123!", "encodedCurrentPassword");
        inOrder.verify(passwordEncoder).encode("NewPassword123!");
        inOrder.verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldSaveUserOnce_WhenPasswordChangeIsSuccessful() {
        // Given
        when(principal.getName()).thenReturn("john.doe@example.com");
        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPassword123!", "encodedCurrentPassword"))
                .thenReturn(true);
        when(passwordEncoder.matches("NewPassword123!", "encodedCurrentPassword"))
                .thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.changePassword(request, principal);

        // Then
        verify(userRepository, times(1)).save(user);
    }
}
