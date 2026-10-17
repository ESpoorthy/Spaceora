package com.spaceora.space.repository;

import com.spaceora.space.entity.Space;
import com.spaceora.space.entity.SpaceType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class SpaceSpecification {

    private SpaceSpecification() {}

    public static Specification<Space> isActive() {
        return (root, query, cb) -> cb.equal(root.get("isActive"), true);
    }

    public static Specification<Space> hasCity(String city) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%");
    }

    public static Specification<Space> hasType(SpaceType type) {
        return (root, query, cb) -> root.get("type").in(type);
    }

    public static Specification<Space> minCapacity(int cap) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("capacity"), cap);
    }

    public static Specification<Space> maxCapacity(int cap) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("capacity"), cap);
    }

    public static Specification<Space> minPrice(BigDecimal p) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("hourlyRate"), p);
    }

    public static Specification<Space> maxPrice(BigDecimal p) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("hourlyRate"), p);
    }
}
