package com.wild.ecommerce.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wild.ecommerce.authentication.JwtService;
import com.wild.ecommerce.category.dto.CategoryDTO;
import com.wild.ecommerce.category.dto.CreateCategoryRequest;
import com.wild.ecommerce.category.dto.UpdateCategoryRequest;
import com.wild.ecommerce.common.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID categoryId1;
    private UUID categoryId2;

    @BeforeEach
    void setUp() {
        categoryId1 = UUID.randomUUID();
        categoryId2 = UUID.randomUUID();
    }

    @Test
    void createCategory_WithValidRequest_ReturnsCreatedStatus() throws Exception {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Test Category",
                "Description for test category"
        );
        CategoryDTO categoryDTO = new CategoryDTO(
                categoryId1,
                "Test Category",
                null
        );

        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(categoryDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("\"" + categoryId1.toString() + "\""));

        verify(categoryService).createCategory(any(CreateCategoryRequest.class));
    }

    @Test
    void createCategory_WithInvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest(
                "",
                "Description for test category"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void getAllCategories_WithDefaultParams_ReturnsPagedCategories() throws Exception {
        // Given
        List<CategoryDTO> categories = List.of(
                new CategoryDTO(categoryId1, "Test Category 1", null),
                new CategoryDTO(categoryId2, "Test Category 2", null)
        );
        PageImpl<CategoryDTO> page = new PageImpl<>(categories, PageRequest.of(0, 20), 2);
        PageResponse<CategoryDTO> pageResponse = new PageResponse<>(page);

        when(categoryService.getAllCategories(any(Pageable.class), isNull()))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(categoryId1.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Test Category 1"));
    }

    @Test
    void getAllCategories_WithCustomParams_UsesCorrectPagination() throws Exception {
        // Given
        PageImpl<CategoryDTO> page = new PageImpl<>(
                List.of(),
                PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "name")),
                0
        );
        PageResponse<CategoryDTO> pageResponse = new PageResponse<>(page);

        when(categoryService.getAllCategories(any(Pageable.class), eq("Test Category")))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/categories")
                        .param("pageNumber", "1")
                        .param("pageSize", "10")
                        .param("sortBy", "name")
                        .param("sortOrder", "DESC")
                        .param("name", "Test Category"))
                .andExpect(status().isOk());

        verify(categoryService).getAllCategories(
                argThat(pageable ->
                        pageable.getPageNumber() == 1 &&
                                pageable.getPageSize() == 10 &&
                                Objects.requireNonNull(pageable.getSort().getOrderFor("name")).getDirection() == Sort.Direction.DESC
                ),
                eq("Test Category")
        );
    }

    @Test
    void getCategoryById_WhenExists_ReturnsCategory() throws Exception {
        // Given
        CategoryDTO categoryDTO = new CategoryDTO(categoryId1, "Test Category", null);

        when(categoryService.getCategoryById(categoryId1)).thenReturn(categoryDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/categories/" + categoryId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId1.toString()))
                .andExpect(jsonPath("$.name").value("Test Category"));
    }

    @Test
    void updateCategory_WithValidRequest_ReturnsUpdatedCategory() throws Exception {
        // Given
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Updated Test Category",
                "Description for test category"
        );
        CategoryDTO updatedDTO = new CategoryDTO(categoryId1, "Updated Test Category", null);

        when(categoryService.updateCategory(eq(categoryId1), any(UpdateCategoryRequest.class)))
                .thenReturn(updatedDTO);

        // When & Then
        mockMvc.perform(patch("/api/v1/categories/" + categoryId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Test Category"));
    }

    @Test
    void deleteCategory_WithValidId_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(categoryService).deleteCategory(categoryId1);

        // When & Then
        mockMvc.perform(delete("/api/v1/categories/" + categoryId1))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(categoryService).deleteCategory(categoryId1);
    }
}
