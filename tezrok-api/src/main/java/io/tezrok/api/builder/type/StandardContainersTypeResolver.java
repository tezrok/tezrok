package io.tezrok.api.builder.type;

import io.tezrok.api.builder.type.resolver.TypeResolver;

import java.util.List;
import java.util.Set;

public class StandardContainersTypeResolver extends ByClassTypeResolver {
    public StandardContainersTypeResolver() {
        this(null);
    }

    public StandardContainersTypeResolver(TypeResolver nextResolver) {
        super(new Class[]{List.class, Set.class}, nextResolver);
    }
}
