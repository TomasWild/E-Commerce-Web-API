package com.wild.ecommerce.category.mapper;

import com.wild.ecommerce.category.dto.CategoryDTO;
import com.wild.ecommerce.category.model.Category;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class CategoryMapper implements Function<Category, CategoryDTO> {

    @Override
    public CategoryDTO apply(Category category) {
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
