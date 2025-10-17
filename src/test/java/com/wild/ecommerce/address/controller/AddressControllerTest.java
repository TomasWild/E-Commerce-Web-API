package com.wild.ecommerce.address.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wild.ecommerce.address.dto.AddressDTO;
import com.wild.ecommerce.address.dto.CreateAddressRequest;
import com.wild.ecommerce.address.dto.UpdateAddressRequest;
import com.wild.ecommerce.address.service.AddressService;
import com.wild.ecommerce.auth.service.JwtService;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.user.model.Role;
import com.wild.ecommerce.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AddressController.class)
public class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User testUser;
    private AddressDTO addressDTO;
    private CreateAddressRequest createRequest;
    private UpdateAddressRequest updateRequest;
    private UUID addressId;

    @BeforeEach
    void setUp() {
        addressId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("password123");
        testUser.setRole(Role.USER);
        testUser.setEnabled(true);

        addressDTO = new AddressDTO(
                addressId,
                "USA",
                "California",
                "San Francisco",
                "123 Market St",
                "94102"
        );

        createRequest = new CreateAddressRequest(
                "USA",
                "California",
                "San Francisco",
                "123 Market St",
                "94102"
        );

        updateRequest = new UpdateAddressRequest(
                "USA",
                "California",
                "Los Angeles",
                "456 Hollywood Blvd",
                "90028"
        );
    }

    @Test
    @WithMockUser
    void createAddress_ShouldReturnCreatedStatus() throws Exception {
        // Arrange
        when(addressService.createAddress(any(CreateAddressRequest.class), eq(testUser.getEmail())))
                .thenReturn(addressDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/addresses")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("\"" + addressId + "\""));

        verify(addressService).createAddress(any(CreateAddressRequest.class), eq(testUser.getEmail()));
    }

    @Test
    @WithMockUser
    void createAddress_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CreateAddressRequest invalidRequest = new CreateAddressRequest(
                "",
                "California",
                "San Francisco",
                "123 Market St",
                "94102"
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/addresses")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAddress_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/addresses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getAllAddresses_ShouldReturnPagedAddresses() throws Exception {
        // Arrange
        List<AddressDTO> addresses = List.of(addressDTO);
        PageResponse<AddressDTO> pageResponse = new PageResponse<>(
                addresses,
                0,
                10,
                1,
                1
        );

        when(addressService.getAllAddresses(
                any(Pageable.class),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(testUser.getEmail())
        )).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/addresses")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.number").value(0));

        verify(addressService).getAllAddresses(
                any(Pageable.class),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(testUser.getEmail())
        );
    }

    @Test
    @WithMockUser
    void getAllAddresses_WithFilters_ShouldReturnFilteredAddresses() throws Exception {
        // Arrange
        List<AddressDTO> addresses = List.of(addressDTO);
        PageResponse<AddressDTO> pageResponse = new PageResponse<>(
                addresses,
                0,
                10,
                1,
                1
        );

        when(addressService.getAllAddresses(
                any(Pageable.class),
                eq("USA"),
                eq("California"),
                eq("San Francisco"),
                eq("94102"),
                eq(testUser.getEmail())
        )).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/addresses")
                        .with(user(testUser))
                        .param("country", "USA")
                        .param("state", "California")
                        .param("city", "San Francisco")
                        .param("postalCode", "94102"))
                .andExpect(status().isOk());

        verify(addressService).getAllAddresses(
                any(Pageable.class),
                eq("USA"),
                eq("California"),
                eq("San Francisco"),
                eq("94102"),
                eq(testUser.getEmail())
        );
    }

    @Test
    @WithMockUser
    void getAllAddresses_WithCustomPagination_ShouldUseProvidedParameters() throws Exception {
        // Arrange
        List<AddressDTO> addresses = List.of(addressDTO);
        PageResponse<AddressDTO> pageResponse = new PageResponse<>(
                addresses,
                1,
                20,
                1,
                1
        );

        Sort sort = Sort.by(Sort.Direction.DESC, "city");
        Pageable expectedPageable = PageRequest.of(1, 20, sort);

        when(addressService.getAllAddresses(
                any(Pageable.class),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(testUser.getEmail())
        )).thenReturn(pageResponse);

        // Act
        mockMvc.perform(get("/api/v1/addresses")
                        .with(user(testUser))
                        .param("pageNumber", "1")
                        .param("pageSize", "20")
                        .param("sortBy", "city")
                        .param("sortOrder", "DESC"))
                .andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(addressService).getAllAddresses(
                pageableCaptor.capture(),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(testUser.getEmail())
        );

        Pageable actualPageable = pageableCaptor.getValue();
        assertThat(actualPageable).isEqualTo(expectedPageable);
    }

    @Test
    void getAllAddresses_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/addresses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getAddressById_ShouldReturnAddress() throws Exception {
        // Arrange
        when(addressService.getAddressById(eq(addressId), eq(testUser.getEmail())))
                .thenReturn(addressDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/addresses/{id}", addressId)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(addressId.toString()))
                .andExpect(jsonPath("$.country").value("USA"))
                .andExpect(jsonPath("$.state").value("California"))
                .andExpect(jsonPath("$.city").value("San Francisco"))
                .andExpect(jsonPath("$.street").value("123 Market St"))
                .andExpect(jsonPath("$.postalCode").value("94102"));

        verify(addressService).getAddressById(eq(addressId), eq(testUser.getEmail()));
    }

    @Test
    void getAddressById_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/addresses/{id}", addressId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void updateAddress_ShouldReturnUpdatedAddress() throws Exception {
        // Arrange
        AddressDTO updatedAddress = new AddressDTO(
                addressId,
                "USA",
                "California",
                "Los Angeles",
                "456 Hollywood Blvd",
                "90028"
        );

        when(addressService.updateAddress(
                eq(addressId),
                any(UpdateAddressRequest.class),
                eq(testUser.getEmail())
        )).thenReturn(updatedAddress);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/addresses/{id}", addressId)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Los Angeles"))
                .andExpect(jsonPath("$.street").value("456 Hollywood Blvd"))
                .andExpect(jsonPath("$.postalCode").value("90028"));

        verify(addressService).updateAddress(
                eq(addressId),
                any(UpdateAddressRequest.class),
                eq(testUser.getEmail())
        );
    }

    @Test
    void updateAddress_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/api/v1/addresses/{id}", addressId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void deleteAddress_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(addressService).deleteAddress(eq(addressId), eq(testUser.getEmail()));

        // Act & Assert
        mockMvc.perform(delete("/api/v1/addresses/{id}", addressId)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(addressService).deleteAddress(eq(addressId), eq(testUser.getEmail()));
    }

    @Test
    void deleteAddress_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/addresses/{id}", addressId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
