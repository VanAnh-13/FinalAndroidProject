package com.example.healthylifehub.utils.filters;

import java.util.List;

/**
 * Strategy Pattern: Defines filtering behavior for lists.
 * Implementations can filter, transform, or sort items.
 *
 * @param <T> the type of items to filter
 */
@FunctionalInterface
public interface FilterStrategy<T> {
    /**
     * Applies the filtering strategy to the source list.
     *
     * @param source the original list
     * @return filtered list (may be the same instance if no filtering needed)
     */
    List<T> apply(List<T> source);
}



