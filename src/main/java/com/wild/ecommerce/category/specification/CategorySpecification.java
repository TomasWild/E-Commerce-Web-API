package com.wild.ecommerce.category.specification;

import com.wild.ecommerce.category.model.Category;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

public final class CategorySpecification {

    private CategorySpecification() {
    }

    public static Specification<Category> filterBy(@Nullable String name) {
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

            return predicate;
        };
    }
}
