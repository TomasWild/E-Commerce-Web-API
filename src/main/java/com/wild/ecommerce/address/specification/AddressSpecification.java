package com.wild.ecommerce.address.specification;

import com.wild.ecommerce.address.model.Address;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class AddressSpecification {

    private AddressSpecification() {
    }

    public static Specification<Address> filterBy(
            @Nullable String country,
            @Nullable String state,
            @Nullable String city,
            @Nullable String postalCode
    ) {
        return (root, _, cb) -> {
            var predicate = cb.conjunction();

            if (country != null && !country.isEmpty()) {
                predicate = cb.and(
                        predicate,
                        cb.like(
                                cb.lower(root.get("country")),
                                "%" + country.toLowerCase() + "%"
                        )
                );
            }

            if (state != null && !state.isEmpty()) {
                predicate = cb.and(
                        predicate,
                        cb.like(
                                cb.lower(root.get("state")),
                                "%" + state.toLowerCase() + "%"
                        )
                );
            }

            if (city != null && !city.isEmpty()) {
                predicate = cb.and(
                        predicate,
                        cb.like(
                                cb.lower(root.get("city")),
                                "%" + city.toLowerCase() + "%"
                        )
                );
            }

            if (postalCode != null && !postalCode.isEmpty()) {
                predicate = cb.and(
                        predicate,
                        cb.like(
                                cb.lower(root.get("postalCode")),
                                "%" + postalCode.toLowerCase() + "%"
                        )
                );
            }

            return predicate;
        };
    }

    public static Specification<Address> belongsToUser(UUID userId) {
        return (root, _, cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }
}
