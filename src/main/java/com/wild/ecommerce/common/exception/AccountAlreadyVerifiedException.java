package com.wild.ecommerce.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AccountAlreadyVerifiedException extends RuntimeException {

    public AccountAlreadyVerifiedException(String message) {
        super(message);
    }
}
