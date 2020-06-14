package io.tezrok.api.builder.type.resolver;


import io.tezrok.api.builder.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Abstract {@link TypeResolver} which supports to call <code>nextResolver</code>
 * if current resolver cannot resolve type itself.
 */
public abstract class BaseTypeResolver implements TypeResolver {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final TypeResolver nextResolver;

    protected BaseTypeResolver(TypeResolver nextResolver) {
        this.nextResolver = nextResolver;
    }

    @Override
    public Type resolveByName(String typeName) {
        log.debug("Try to resolve type '{}' by {}", typeName, getClass().getName());
        
        Optional<Type> foundType = tryResolveByName(typeName);

        return foundType.orElseThrow(() ->
                new RuntimeException(String.format("Type cannot be resolved: '%s'", typeName)));
    }

    /**
     * In overriding method, please, call this method at the end.
     *
     * @param typeName Name of the type.
     * @return
     */
    @Override
    public Optional<Type> tryResolveByName(String typeName) {
        if (nextResolver != null) {
            // may be next resolver know about this type
            log.debug(String.format("Try to resolve type '%s' by %s", typeName, nextResolver.getClass().getName()));
            return nextResolver.tryResolveByName(typeName);
        }

        return Optional.empty();
    }
}
