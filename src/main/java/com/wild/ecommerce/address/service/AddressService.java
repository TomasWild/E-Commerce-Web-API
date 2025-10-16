package com.wild.ecommerce.address.service;

import com.wild.ecommerce.address.dto.AddressDTO;
import com.wild.ecommerce.address.dto.CreateAddressRequest;
import com.wild.ecommerce.address.dto.UpdateAddressRequest;
import com.wild.ecommerce.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AddressService {

    AddressDTO createAddress(CreateAddressRequest request, String userEmail);

    PageResponse<AddressDTO> getAllAddresses(
            Pageable pageable,
            String country,
            String state,
            String city,
            String postalCode,
            String userEmail
    );

    AddressDTO getAddressById(UUID id, String userEmail);

    AddressDTO updateAddress(UUID id, UpdateAddressRequest request, String userEmail);

    void deleteAddress(UUID id, String userEmail);
}
