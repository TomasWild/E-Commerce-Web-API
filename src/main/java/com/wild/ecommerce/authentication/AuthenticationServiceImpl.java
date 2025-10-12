package com.wild.ecommerce.authentication;

import com.wild.ecommerce.authentication.dto.AuthenticationRequest;
import com.wild.ecommerce.authentication.dto.AuthenticationResponse;
import com.wild.ecommerce.authentication.dto.RegisterRequest;
import com.wild.ecommerce.common.exception.AccountAlreadyVerifiedException;
import com.wild.ecommerce.common.exception.InvalidTokenException;
import com.wild.ecommerce.common.exception.ResourceAlreadyExistsException;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.notification.EmailService;
import com.wild.ecommerce.user.Role;
import com.wild.ecommerce.user.User;
import com.wild.ecommerce.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new ResourceAlreadyExistsException("User with email '" + request.email() + "' already exists");
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEnabled(false);

        userRepository.save(user);

        String token = generateVerificationToken(user);
        emailService.sendVerificationEmail(user.getFirstName(), user.getEmail(), token);

        return "Registration successful. Please check your email to verify your account";
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (DisabledException e) {
            log.error("User with email '{}' is disabled", request.email());
            throw new DisabledException("User with email '" + request.email() + "' is disabled");
        }

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + request.email() + "' not found"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthenticationResponse(
                user.getEmail(),
                user.getRole().name(),
                accessToken,
                refreshToken,
                TokenType.BEARER
        );
    }

    @Override
    @Transactional
    public String verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Verification token not found"));

        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("Token already used");
        }

        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("Token expired. Please request a new verification email");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        return "Email verified successfully. You can now login";
    }

    @Override
    @Transactional
    public String resendVerificationEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + email + "' not found"));

        if (user.isEnabled()) {
            throw new AccountAlreadyVerifiedException("Account already verified");
        }

        String token = generateVerificationToken(user);
        emailService.sendVerificationEmail(user.getFirstName(), user.getEmail(), token);

        return "Verification email sent. Please check your inbox";
    }

    private String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        verificationTokenRepository.save(verificationToken);

        return token;
    }
}
