package com.wild.ecommerce.category;

import org.springframework.data.jpa.domain.Specification;

public final class CategorySpecification {

    private CategorySpecification() {
    }

    public static Specification<Category> filterBy(String name) {
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
