package com.wild.ecommerce.authentication;

import com.wild.ecommerce.authentication.dto.AuthenticationRequest;
import com.wild.ecommerce.authentication.dto.AuthenticationResponse;
import com.wild.ecommerce.authentication.dto.RegisterRequest;

public interface AuthenticationService {

    String register(RegisterRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    String verifyEmail(String token);

    String resendVerificationEmail(String email);
}
