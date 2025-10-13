package com.wild.ecommerce.product.specification;

import com.wild.ecommerce.product.model.Product;
import jakarta.persistence.criteria.JoinType;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> filterBy(
            @Nullable String name,
            @Nullable String brand,
            @Nullable UUID categoryId
    ) {
        return (root, _, cb) -> {
            var predicate = cb.conjunction();

            if (name != null && !name.isEmpty()) {
                predicate = cb.and(
                        predicate,
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + name.toLowerCase() + "%"
                        )
                );
            }

            if (brand != null && !brand.isEmpty()) {
                predicate = cb.and(
                        predicate,
                        cb.like(
                                cb.lower(root.get("brand")),
                                "%" + brand.toLowerCase() + "%"
                        )
                );
            }

            if (categoryId != null) {
                var categoryJoin = root.join("category", JoinType.INNER);
                predicate = cb.and(
                        predicate,
                        cb.equal(categoryJoin.get("id"), categoryId)
                );
            }

            return predicate;
        };
    }
}
