package com.wild.ecommerce.category.service;

import com.wild.ecommerce.category.repository.CategoryRepository;
import com.wild.ecommerce.category.specification.CategorySpecification;
import com.wild.ecommerce.category.dto.CategoryDTO;
import com.wild.ecommerce.category.mapper.CategoryMapper;
import com.wild.ecommerce.category.dto.CreateCategoryRequest;
import com.wild.ecommerce.category.dto.UpdateCategoryRequest;
import com.wild.ecommerce.category.model.Category;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.common.exception.ResourceAlreadyExistsException;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.common.util.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDTO createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            log.warn("Failed to create category - name '{}' already exists", request.name());
            throw new ResourceAlreadyExistsException("Category with name '" + request.name() + "' already exists");
        }

        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.apply(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "categories",
            key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + (#name != null ? #name : 'all')"
    )
    public PageResponse<CategoryDTO> getAllCategories(Pageable pageable, String name) {
        Specification<Category> spec = CategorySpecification.filterBy(name);

        Page<CategoryDTO> categoryPage = categoryRepository.findAll(spec, pageable)
                .map(categoryMapper);

        return new PageResponse<>(categoryPage);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "category", key = "#id")
    public CategoryDTO getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found with ID: {}", id);
                    return new ResourceNotFoundException("Category with ID '" + id + "' not found");
                });

        return categoryMapper.apply(category);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "category", key = "#id"),
                    @CacheEvict(value = "categories", allEntries = true)
            }
    )
    public CategoryDTO updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Failed to update - category not found with ID: {}", id);
                    return new ResourceNotFoundException("Category with ID '" + id + "' not found");
                });

        if (!category.getName().equalsIgnoreCase(request.name()) &&
                categoryRepository.existsByNameIgnoreCase(request.name())
        ) {
            log.warn("Failed to update category {} - name '{}' already exists", id, request.name());
            throw new ResourceAlreadyExistsException("Category with name '" + request.name() + "' already exists");
        }

        BeanUtil.copyNonNullProperties(request, category);

        Category updatedCategory = categoryRepository.save(category);

        return categoryMapper.apply(updatedCategory);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "category", key = "#id"),
                    @CacheEvict(value = "categories", allEntries = true)
            }
    )
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Failed to delete - category not found with ID: {}", id);
                    return new ResourceNotFoundException("Category with ID '" + id + "' not found");
                });

        categoryRepository.delete(category);
    }
}
