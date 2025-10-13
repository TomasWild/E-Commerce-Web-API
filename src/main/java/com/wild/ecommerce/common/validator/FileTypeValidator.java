package com.wild.ecommerce.common.validator;

import com.wild.ecommerce.common.util.FileType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

public class FileTypeValidator implements ConstraintValidator<FileType, MultipartFile> {

    private List<String> allowedTypes;

    @Override
    public void initialize(FileType constraintAnnotation) {
        this.allowedTypes = Arrays.asList(constraintAnnotation.allowed());
    }

    @Override
    public boolean isValid(MultipartFile value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        String contentType = value.getContentType();

        if (contentType != null && allowedTypes.contains(contentType)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "File type must be one of: " + String.join(", ", allowedTypes)
        ).addConstraintViolation();

        return false;
    }
}
