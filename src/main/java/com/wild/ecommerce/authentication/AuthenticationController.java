package com.wild.ecommerce.authentication;

import com.wild.ecommerce.authentication.dto.AuthenticationRequest;
import com.wild.ecommerce.authentication.dto.AuthenticationResponse;
import com.wild.ecommerce.authentication.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        var response = authenticationService.register(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        var response = authenticationService.authenticate(request);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam(value = "token") String token) {
        var response = authenticationService.verifyEmail(token);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerificationEmail(@RequestParam(value = "email") String email) {
        var response = authenticationService.resendVerificationEmail(email);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
