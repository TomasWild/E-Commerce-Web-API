package com.wild.ecommerce.product.mapper;

import com.wild.ecommerce.product.dto.ProductDTO;
import com.wild.ecommerce.product.model.Product;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ProductMapper implements Function<Product, ProductDTO> {

    @Override
    public ProductDTO apply(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.getCategory().getName()
        );
    }
}
