package com.wild.ecommerce.product.dto;

import com.wild.ecommerce.common.util.FileSize;
import com.wild.ecommerce.common.util.FileType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
        String name,

        @Size(min = 2, max = 50, message = "Brand must be between 2 and 50 characters")
        String brand,

        @Size(max = 1000, message = "Description must be less than 1000 characters")
        String description,

        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,

        @Min(value = 0, message = "Stock cannot be negative")
        int stock,

        @FileType(allowed = {"image/jpeg", "image/jpg", "image/png"}, message = "Only JPEG, JPG and PNG files are allowed")
        @FileSize(min = 50 * 1024, max = 10 * 1024 * 1024, message = "Image must be between 50KB and 10MB")
        MultipartFile image,

        UUID categoryId
) {
}
