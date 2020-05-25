package io.tezrok.api.builder.type;

import io.tezrok.api.builder.type.resolver.BaseTypeResolver;
import io.tezrok.api.builder.type.resolver.TypeResolver;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ByClassTypeResolver extends BaseTypeResolver {
    private final List<Class> classes;

    public ByClassTypeResolver(Class clazz) {
        this(new Class[]{clazz});
    }

    public ByClassTypeResolver(Class[] classes) {
        this(classes, null);
    }

    public ByClassTypeResolver(Class[] classes, TypeResolver nextResolver) {
        super(nextResolver);
        this.classes = Arrays.asList(Validate.notNull(classes, "classes"));
    }

    @Override
    public Optional<Type> tryResolveByName(String typeName) {
        Optional<Class> foundClass = classes.stream()
                .filter(c -> c.getSimpleName().equals(typeName))
                .findFirst();

        if (foundClass.isPresent()) {
            return Optional.of(new NamedType(foundClass.get()));
        }

        return super.tryResolveByName(typeName);
    }
}
