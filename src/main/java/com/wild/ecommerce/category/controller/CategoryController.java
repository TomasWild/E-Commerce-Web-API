package com.wild.ecommerce.category.controller;

import com.wild.ecommerce.category.service.CategoryService;
import com.wild.ecommerce.category.dto.CategoryDTO;
import com.wild.ecommerce.category.dto.CreateCategoryRequest;
import com.wild.ecommerce.category.dto.UpdateCategoryRequest;
import com.wild.ecommerce.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Endpoints for managing categories")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<UUID> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryDTO category = categoryService.createCategory(request);

        return new ResponseEntity<>(category.id(), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<PagedModel<CategoryDTO>> getAllCategories(
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = "ASC") String sortOrder,
            @RequestParam(value = "name", required = false) String name
    ) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        PageResponse<CategoryDTO> categoriesResponse = categoryService.getAllCategories(pageable, name);
        Page<CategoryDTO> categories = categoriesResponse.toPage();

        return new ResponseEntity<>(new PagedModel<>(categories), HttpStatus.OK);
    }

    @GetMapping("{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable("id") UUID id) {
        CategoryDTO category = categoryService.getCategoryById(id);

        return new ResponseEntity<>(category, HttpStatus.OK);
    }

    @PatchMapping("{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryDTO category = categoryService.updateCategory(id, request);

        return new ResponseEntity<>(category, HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") UUID id) {
        categoryService.deleteCategory(id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
