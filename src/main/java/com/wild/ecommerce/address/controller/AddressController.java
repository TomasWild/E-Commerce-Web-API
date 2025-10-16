package com.wild.ecommerce.address.controller;

import com.wild.ecommerce.address.dto.AddressDTO;
import com.wild.ecommerce.address.dto.CreateAddressRequest;
import com.wild.ecommerce.address.dto.UpdateAddressRequest;
import com.wild.ecommerce.address.service.AddressService;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.user.model.User;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Endpoints for managing addresses")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<UUID> createAddress(
            @Valid @RequestBody CreateAddressRequest request,
            @AuthenticationPrincipal User user
    ) {
        AddressDTO address = addressService.createAddress(request, user.getEmail());

        return new ResponseEntity<>(address.id(), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<PagedModel<AddressDTO>> getAllAddresses(
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = "ASC") String sortOrder,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "postalCode", required = false) String postalCode,
            @AuthenticationPrincipal User user
    ) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        PageResponse<AddressDTO> addressesResponse =
                addressService.getAllAddresses(pageable, country, state, city, postalCode, user.getEmail());
        Page<AddressDTO> addresses = addressesResponse.toPage();

        return new ResponseEntity<>(new PagedModel<>(addresses), HttpStatus.OK);
    }

    @GetMapping("{id}")
    public ResponseEntity<AddressDTO> getAddressById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal User user
    ) {
        AddressDTO address = addressService.getAddressById(id, user.getEmail());

        return new ResponseEntity<>(address, HttpStatus.OK);
    }

    @PatchMapping("{id}")
    public ResponseEntity<AddressDTO> updateAddress(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAddressRequest request,
            @AuthenticationPrincipal User user
    ) {
        AddressDTO address = addressService.updateAddress(id, request, user.getEmail());

        return new ResponseEntity<>(address, HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal User user
    ) {
        addressService.deleteAddress(id, user.getEmail());

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
