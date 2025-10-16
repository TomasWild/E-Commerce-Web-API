package com.wild.ecommerce.address.service;

import com.wild.ecommerce.address.dto.AddressDTO;
import com.wild.ecommerce.address.dto.CreateAddressRequest;
import com.wild.ecommerce.address.dto.UpdateAddressRequest;
import com.wild.ecommerce.address.mapper.AddressMapper;
import com.wild.ecommerce.address.model.Address;
import com.wild.ecommerce.address.repository.AddressRepository;
import com.wild.ecommerce.address.specification.AddressSpecification;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.common.util.BeanUtil;
import com.wild.ecommerce.user.model.User;
import com.wild.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public AddressDTO createAddress(CreateAddressRequest request, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: '" + userEmail + "' not found"));

        Address address = new Address();
        address.setCountry(request.country());
        address.setState(request.state());
        address.setCity(request.city());
        address.setStreet(request.street());
        address.setPostalCode(request.postalCode());
        address.setUser(user);

        Address savedAddress = addressRepository.save(address);

        return addressMapper.apply(savedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AddressDTO> getAllAddresses(
            Pageable pageable,
            String country,
            String state,
            String city,
            String postalCode,
            String userEmail
    ) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: '" + userEmail + "' not found"));

        Specification<Address> spec = AddressSpecification.filterBy(country, state, city, postalCode)
                .and(AddressSpecification.belongsToUser(user.getId()));

        Page<AddressDTO> page = addressRepository.findAll(spec, pageable)
                .map(addressMapper);
        log.debug("Retrieved {} addresses out of {} total", page.getNumberOfElements(), page.getTotalElements());

        return new PageResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDTO getAddressById(UUID id, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: '" + userEmail + "' not found"));

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Address not found with ID: {}", id);
                    return new ResourceNotFoundException("Address with ID '" + id + "' not found");
                });

        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address not found or access denied");
        }

        return addressMapper.apply(address);
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(UUID id, UpdateAddressRequest request, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: '" + userEmail + "' not found"));

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Failed to update - address not found with ID: {}", id);
                    return new ResourceNotFoundException("Address with ID '" + id + "' not found");
                });

        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address not found or access denied");
        }

        BeanUtil.copyNonNullProperties(request, address);

        Address updatedAddress = addressRepository.save(address);

        return addressMapper.apply(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID id, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: '" + userEmail + "' not found"));

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Failed to delete - address not found with ID: {}", id);
                    return new ResourceNotFoundException("Address with ID '" + id + "' not found");
                });

        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address not found or access denied");
        }

        addressRepository.delete(address);
    }
}
