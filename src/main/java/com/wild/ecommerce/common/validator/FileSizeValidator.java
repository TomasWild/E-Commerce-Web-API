package com.wild.ecommerce.common.validator;

import com.wild.ecommerce.common.util.FileSize;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class FileSizeValidator implements ConstraintValidator<FileSize, MultipartFile> {

    private long maxSize;
    private long minSize;

    @Override
    public void initialize(FileSize constraintAnnotation) {
        this.maxSize = constraintAnnotation.max();
        this.minSize = constraintAnnotation.min();
    }

    @Override
    public boolean isValid(MultipartFile value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        long fileSize = value.getSize();

        if (fileSize < minSize) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "File size must be at least " + formatBytes(minSize)
            ).addConstraintViolation();

            return false;
        }

        if (fileSize > maxSize) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "File size must not exceed " + formatBytes(maxSize)
            ).addConstraintViolation();

            return false;
        }

        return true;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        }

        return (bytes / (1024 * 1024)) + " MB";
    }
}
