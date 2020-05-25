package io.tezrok.api.builder.type.resolver;

import io.tezrok.api.builder.type.Type;

import java.util.Optional;

/**
 * Type resolver.
 */
public interface TypeResolver {
    /**
     * Returns {@link Type} by name.
     *
     * @param typeName Name of the type.
     * @return Not null instance of {@link Type}.
     * @throws RuntimeException when type not found.
     */
    Type resolveByName(String typeName);

    /**
     * Tries to return {@link Type} by name.
     *
     * @param typeName Name of the type.
     * @return Not null value.
     */
    Optional<Type> tryResolveByName(String typeName);
}
