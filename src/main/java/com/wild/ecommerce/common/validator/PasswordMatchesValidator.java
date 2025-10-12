package com.wild.ecommerce.common.validator;

import com.wild.ecommerce.authentication.dto.RegisterRequest;
import com.wild.ecommerce.common.util.PasswordMatches;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegisterRequest> {

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(RegisterRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String password = value.password();
        String confirmPassword = value.confirmPassword();

        if (password == null || confirmPassword == null) {
            return true;
        }

        boolean isValid = password.equals(confirmPassword);

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }

        return isValid;
    }
}
