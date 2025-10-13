package com.wild.ecommerce.user.controller;

import com.wild.ecommerce.user.service.UserService;
import com.wild.ecommerce.user.dto.ChangePasswordRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for user account management")
public class UserController {

    private final UserService userService;

    @PatchMapping
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal
    ) {
        userService.changePassword(request, principal);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
