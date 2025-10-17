package com.wild.ecommerce.address.service;

import com.wild.ecommerce.address.dto.AddressDTO;
import com.wild.ecommerce.address.dto.CreateAddressRequest;
import com.wild.ecommerce.address.dto.UpdateAddressRequest;
import com.wild.ecommerce.address.mapper.AddressMapper;
import com.wild.ecommerce.address.model.Address;
import com.wild.ecommerce.address.repository.AddressRepository;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.user.model.User;
import com.wild.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User user;
    private Address address;
    private AddressDTO addressDTO;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();
    private static final String USER_EMAIL_TEST = "user.test@example.com";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL_TEST);

        address = new Address();
        address.setId(ADDRESS_ID);
        address.setCountry("USA");
        address.setState("California");
        address.setCity("San Francisco");
        address.setStreet("123 Main St");
        address.setPostalCode("94102");
        address.setUser(user);

        addressDTO = new AddressDTO(
                ADDRESS_ID,
                "USA",
                "California",
                "San Francisco",
                "123 Main St",
                "94102"
        );
    }

    @Test
    void createAddress_Success() {
        // Arrange
        CreateAddressRequest request = new CreateAddressRequest(
                "USA",
                "California",
                "San Francisco",
                "123 Main St",
                "94102"
        );

        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.of(user));
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        when(addressMapper.apply(address)).thenReturn(addressDTO);

        // Act
        AddressDTO result = addressService.createAddress(request, USER_EMAIL_TEST);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.country()).isEqualTo("USA");
        assertThat(result.city()).isEqualTo("San Francisco");

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(addressCaptor.capture());

        Address savedAddress = addressCaptor.getValue();
        assertThat(savedAddress.getCountry()).isEqualTo("USA");
        assertThat(savedAddress.getUser()).isEqualTo(user);
    }

    @Test
    void createAddress_UserNotFound() {
        // Arrange
        CreateAddressRequest request = new CreateAddressRequest(
                "USA",
                "California",
                "San Francisco",
                "123 Main St",
                "94102"
        );

        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.createAddress(request, USER_EMAIL_TEST))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with email: '" + USER_EMAIL_TEST + "' not found");

        verify(addressRepository, never()).save(any());
    }

    @Test
    void getAllAddresses_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Address> addressPage = new PageImpl<>(List.of(address));

        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.of(user));
        when(addressRepository.findAll(ArgumentMatchers.<Specification<Address>>any(), eq(pageable)))
                .thenReturn(addressPage);
        when(addressMapper.apply(address)).thenReturn(addressDTO);

        // Act
        PageResponse<AddressDTO> result = addressService.getAllAddresses(
                pageable,
                "USA",
                "California",
                "San Francisco",
                "94102",
                USER_EMAIL_TEST
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().country()).isEqualTo("USA");
        verify(addressRepository).findAll(ArgumentMatchers.<Specification<Address>>any(), eq(pageable));
    }

    @Test
    void getAllAddresses_UserNotFound() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.getAllAddresses(
                pageable, null, null, null, null, USER_EMAIL_TEST
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with email: '" + USER_EMAIL_TEST + "' not found");
    }

    @Test
    void getAddressById_Success() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.of(user));
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
        when(addressMapper.apply(address)).thenReturn(addressDTO);

        // Act
        AddressDTO result = addressService.getAddressById(ADDRESS_ID, USER_EMAIL_TEST);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(ADDRESS_ID);
        assertThat(result.country()).isEqualTo("USA");
    }

    @Test
    void getAddressById_AddressNotFound() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.of(user));
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.getAddressById(ADDRESS_ID, USER_EMAIL_TEST))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address with ID '" + ADDRESS_ID + "' not found");
    }

    @Test
    void getAddressById_AccessDenied() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID());
        anotherUser.setEmail("another@example.com");

        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.of(user));
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));

        address.setUser(anotherUser);

        // Act & Assert
        assertThatThrownBy(() -> addressService.getAddressById(ADDRESS_ID, USER_EMAIL_TEST))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found or access denied");
    }

    @Test
    void updateAddress_Success() {
        // Arrange
        UpdateAddressRequest request = new UpdateAddressRequest(
                "Canada", "Ontario", "Toronto", "456 New St", "M5H 2N2"
        );

        Address updatedAddress = new Address();
        updatedAddress.setId(ADDRESS_ID);
        updatedAddress.setCountry("Canada");
        updatedAddress.setState("Ontario");
        updatedAddress.setCity("Toronto");
        updatedAddress.setStreet("456 New St");
        updatedAddress.setPostalCode("M5H 2N2");
        updatedAddress.setUser(user);

        AddressDTO updatedDTO = new AddressDTO(
                ADDRESS_ID, "Canada", "Ontario", "Toronto", "456 New St", "M5H 2N2"
        );

        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.of(user));
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);
        when(addressMapper.apply(updatedAddress)).thenReturn(updatedDTO);

        // Act
        AddressDTO result = addressService.updateAddress(ADDRESS_ID, request, USER_EMAIL_TEST);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.country()).isEqualTo("Canada");
        assertThat(result.city()).isEqualTo("Toronto");
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    void updateAddress_AddressNotFound() {
        // Arrange
        UpdateAddressRequest request = new UpdateAddressRequest(
                "Canada", "Ontario", "Toronto", "456 New St", "M5H 2N2"
        );

        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.of(user));
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.updateAddress(ADDRESS_ID, request, USER_EMAIL_TEST))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address with ID '" + ADDRESS_ID + "' not found");

        verify(addressRepository, never()).save(any());
    }

    @Test
    void updateAddress_AccessDenied() {
        // Arrange
        UpdateAddressRequest request = new UpdateAddressRequest(
                "Canada", "Ontario", "Toronto", "456 New St", "M5H 2N2"
        );

        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID());
        address.setUser(anotherUser);

        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.of(user));
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));

        // Act & Assert
        assertThatThrownBy(() -> addressService.updateAddress(ADDRESS_ID, request, USER_EMAIL_TEST))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found or access denied");

        verify(addressRepository, never()).save(any());
    }

    @Test
    void deleteAddress_Success() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.of(user));
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));

        // Act
        addressService.deleteAddress(ADDRESS_ID, USER_EMAIL_TEST);

        // Assert
        verify(addressRepository).delete(address);
    }

    @Test
    void deleteAddress_AddressNotFound() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.of(user));
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.deleteAddress(ADDRESS_ID, USER_EMAIL_TEST))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address with ID '" + ADDRESS_ID + "' not found");

        verify(addressRepository, never()).delete((Address) any());
    }

    @Test
    void deleteAddress_AccessDenied() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID());
        address.setUser(anotherUser);

        when(userRepository.findByEmailIgnoreCase(USER_EMAIL_TEST)).thenReturn(Optional.of(user));
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));

        // Act & Assert
        assertThatThrownBy(() -> addressService.deleteAddress(ADDRESS_ID, USER_EMAIL_TEST))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found or access denied");

        verify(addressRepository, never()).delete((Address) any());
    }
}
