package io.tezrok.api.builder.type;

import io.tezrok.api.builder.type.resolver.BaseTypeResolver;
import io.tezrok.api.builder.type.resolver.TypeResolver;

import java.util.Optional;

public class PrimitiveTypeResolver extends BaseTypeResolver {
    public PrimitiveTypeResolver() {
        this(null);
    }

    protected PrimitiveTypeResolver(TypeResolver nextResolver) {
        super(nextResolver);
    }

    @Override
    public Optional<Type> tryResolveByName(String typeName) {
        if (PrimitiveType.isPrimitive(typeName)) {
            return Optional.of(new PrimitiveType(typeName));
        }

        return super.tryResolveByName(typeName);
    }
}
