package com.wild.ecommerce.address.mapper;

import com.wild.ecommerce.address.dto.AddressDTO;
import com.wild.ecommerce.address.model.Address;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class AddressMapper implements Function<Address, AddressDTO> {

    @Override
    public AddressDTO apply(Address address) {
        return new AddressDTO(
                address.getId(),
                address.getCountry(),
                address.getState(),
                address.getCity(),
                address.getStreet(),
                address.getPostalCode()
        );
    }
}
