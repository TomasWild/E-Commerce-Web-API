package com.wild.ecommerce.auth.service;

import com.wild.ecommerce.auth.dto.AuthenticationRequest;
import com.wild.ecommerce.auth.dto.AuthenticationResponse;
import com.wild.ecommerce.auth.dto.RegisterRequest;

public interface AuthenticationService {

    String register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    String verifyEmail(String token);

    String resendVerificationEmail(String email);
}
