package io.tezrok.api.builder.type.resolver;

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
