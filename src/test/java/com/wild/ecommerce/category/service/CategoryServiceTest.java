package com.wild.ecommerce.category.service;

import com.wild.ecommerce.category.dto.CategoryDTO;
import com.wild.ecommerce.category.mapper.CategoryMapper;
import com.wild.ecommerce.category.dto.CreateCategoryRequest;
import com.wild.ecommerce.category.dto.UpdateCategoryRequest;
import com.wild.ecommerce.category.model.Category;
import com.wild.ecommerce.category.repository.CategoryRepository;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.common.exception.ResourceAlreadyExistsException;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private UUID id;
    private Category category;
    private CategoryDTO categoryDTO;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();

        category = new Category();
        category.setId(id);
        category.setName("Category Test Name");
        category.setDescription("Category Test description");

        categoryDTO = new CategoryDTO(
                id,
                "Category Test Name",
                "Category Test description"
        );
    }

    @Test
    void createCategory_ShouldCreateSuccessfully_WhenNameDoesNotExist() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Category Test Name",
                "Category Test description"
        );

        when(categoryRepository.existsByNameIgnoreCase("Category Test Name")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.apply(category)).thenReturn(categoryDTO);

        // When
        CategoryDTO result = categoryService.createCategory(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Category Test Name");

        verify(categoryRepository).existsByNameIgnoreCase("Category Test Name");
        verify(categoryRepository).save(any(Category.class));
        verify(categoryMapper).apply(category);
    }

    @Test
    void createCategory_ShouldThrowException_WhenNameAlreadyExists() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Category Test Name",
                "Category Test description"
        );

        when(categoryRepository.existsByNameIgnoreCase("Category Test Name")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Category with name 'Category Test Name' already exists");

        verify(categoryRepository).existsByNameIgnoreCase("Category Test Name");
        verify(categoryRepository, never()).save(any());
        verify(categoryMapper, never()).apply(any());
    }

    @Test
    void getAllCategories_ShouldReturnPagedCategories_WithoutFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> categoryPage = new PageImpl<>(List.of(category));

        when(categoryRepository.findAll(ArgumentMatchers.<Specification<Category>>any(), eq(pageable)))
                .thenReturn(categoryPage);
        when(categoryMapper.apply(category)).thenReturn(categoryDTO);

        // When
        PageResponse<CategoryDTO> result = categoryService.getAllCategories(pageable, "");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Category Test Name");

        verify(categoryRepository).findAll(ArgumentMatchers.<Specification<Category>>any(), eq(pageable));
    }

    @Test
    void getAllCategories_ShouldReturnFilteredCategories_WithNameFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        String nameFilter = "Tes";
        Page<Category> categoryPage = new PageImpl<>(List.of(category));

        when(categoryRepository.findAll(ArgumentMatchers.<Specification<Category>>any(), eq(pageable)))
                .thenReturn(categoryPage);
        when(categoryMapper.apply(category)).thenReturn(categoryDTO);

        // When
        PageResponse<CategoryDTO> result = categoryService.getAllCategories(pageable, nameFilter);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(1);

        verify(categoryRepository).findAll(ArgumentMatchers.<Specification<Category>>any(), eq(pageable));
    }

    @Test
    void getCategoryById_ShouldReturnCategory_WhenExists() {
        // Given
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryMapper.apply(category)).thenReturn(categoryDTO);

        // When
        CategoryDTO result = categoryService.getCategoryById(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Category Test Name");

        verify(categoryRepository).findById(id);
        verify(categoryMapper).apply(category);
    }

    @Test
    void getCategoryById_ShouldThrowException_WhenNotFound() {
        // Given
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.getCategoryById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category with ID '" + id + "' not found");

        verify(categoryRepository).findById(id);
        verify(categoryMapper, never()).apply(any());
    }

    @Test
    void updateCategory_ShouldUpdateSuccessfully_WhenNameIsUnchanged() {
        // Given
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Category Test Name",
                "Category Test description"
        );

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.apply(category)).thenReturn(categoryDTO);

        // When
        CategoryDTO result = categoryService.updateCategory(id, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Category Test Name");

        verify(categoryRepository).findById(id);
        verify(categoryRepository, never()).existsByNameIgnoreCase(anyString());
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_ShouldUpdateSuccessfully_WhenNewNameDoesNotExist() {
        // Given
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Update Category Test Name",
                "Update Category Test description"
        );

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCase("Update Category Test Name")).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.apply(category)).thenReturn(
                new CategoryDTO(id, "Update Category Test Name", "Update Category Test description")
        );

        // When
        CategoryDTO result = categoryService.updateCategory(id, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Update Category Test Name");

        verify(categoryRepository).findById(id);
        verify(categoryRepository).existsByNameIgnoreCase("Update Category Test Name");
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_ShouldThrowException_WhenNewNameAlreadyExists() {
        // Given
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Update Category Test Name",
                "Update Category Test description"
        );

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCase("Update Category Test Name")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(id, request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Category with name 'Update Category Test Name' already exists");

        verify(categoryRepository).findById(id);
        verify(categoryRepository).existsByNameIgnoreCase("Update Category Test Name");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_ShouldThrowException_WhenCategoryNotFound() {
        // Given
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Update Category Test Name",
                "Update Category Test description"
        );

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(id, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category with ID '" + id + "' not found");

        verify(categoryRepository).findById(id);
        verify(categoryRepository, never()).existsByNameIgnoreCase(anyString());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_ShouldDeleteSuccessfully_WhenExists() {
        // Given
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        // When
        categoryService.deleteCategory(id);

        // Then
        verify(categoryRepository).findById(id);
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_ShouldThrowException_WhenNotFound() {
        // Given
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.deleteCategory(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category with ID '" + id + "' not found");

        verify(categoryRepository).findById(id);
        verify(categoryRepository, never()).delete((Category) any());
    }
}
