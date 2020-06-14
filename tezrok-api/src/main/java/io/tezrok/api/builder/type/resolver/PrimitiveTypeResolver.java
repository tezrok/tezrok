package io.tezrok.api.builder.type.resolver;


import io.tezrok.api.builder.type.PrimitiveType;
import io.tezrok.api.builder.type.Type;

import java.util.Optional;

public class PrimitiveTypeResolver extends BaseTypeResolver {
    public PrimitiveTypeResolver() {
        this(null);
    }

    public PrimitiveTypeResolver(TypeResolver nextResolver) {
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
