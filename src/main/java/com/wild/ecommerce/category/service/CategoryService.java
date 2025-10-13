package com.wild.ecommerce.category.service;

import com.wild.ecommerce.category.dto.CategoryDTO;
import com.wild.ecommerce.category.dto.CreateCategoryRequest;
import com.wild.ecommerce.category.dto.UpdateCategoryRequest;
import com.wild.ecommerce.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CategoryService {

    CategoryDTO createCategory(CreateCategoryRequest request);

    PageResponse<CategoryDTO> getAllCategories(Pageable pageable, String name);

    CategoryDTO getCategoryById(UUID id);

    CategoryDTO updateCategory(UUID id, UpdateCategoryRequest request);

    void deleteCategory(UUID id);
}
