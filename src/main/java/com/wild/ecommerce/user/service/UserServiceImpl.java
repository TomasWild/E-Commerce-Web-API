package com.wild.ecommerce.user.service;

import com.wild.ecommerce.common.exception.InvalidPasswordException;
import com.wild.ecommerce.user.dto.ChangePasswordRequest;
import com.wild.ecommerce.user.model.User;
import com.wild.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> {
                    log.error("Password change failed - user not found: {}", username);
                    return new UsernameNotFoundException("User not found");
                });

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            log.warn("Password change failed for user: {} - incorrect current password", username);
            throw new InvalidPasswordException("Wrong password");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            log.warn("Password change failed for user: {} - new password same as current", username);
            throw new InvalidPasswordException("New password must be different from current password");
        }

        if (!request.confirmNewPassword().equals(request.newPassword())) {
            log.warn("Password change failed for user: {} - password confirmation mismatch", username);
            throw new InvalidPasswordException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        userRepository.save(user);
    }
}
