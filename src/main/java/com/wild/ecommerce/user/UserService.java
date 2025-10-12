package com.wild.ecommerce.user;

import com.wild.ecommerce.user.dto.ChangePasswordRequest;

import java.security.Principal;

public interface UserService {

    void changePassword(ChangePasswordRequest request, Principal principal);
}
