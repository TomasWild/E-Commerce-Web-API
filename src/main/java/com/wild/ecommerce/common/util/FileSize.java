package com.wild.ecommerce.common.util;

import com.wild.ecommerce.common.validator.FileSizeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to validate file size constraints on uploaded files.
 * It ensures that the size of the file falls within a specific range defined by
 * the {@code min} and {@code max} attributes.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FileSizeValidator.class)
public @interface FileSize {

    String message() default "File size must be between 0 and 5MB";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long max() default Long.MAX_VALUE;

    long min() default 0;
}
