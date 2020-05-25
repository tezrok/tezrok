package io.tezrok.api.builder.type;

import io.tezrok.api.builder.type.resolver.TypeResolver;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenericType extends Type {
    private final Type parameterType;
    private final Type containerType;

    public GenericType(String name, Class... classes) {
        this(name, new ByClassTypeResolver(classes, new PrimitiveTypeResolver()));
    }

    public GenericType(String name, TypeResolver typeResolver) {
        super(name, StringUtils.EMPTY);
        GenericInfo info = parseGeneric(name, Validate.notNull(typeResolver, "typeResolver"));
        this.containerType = info.containerType;
        this.parameterType = info.parameterType;
    }

    @Override
    public boolean isGeneric() {
        return true;
    }

    @Override
    public List<String> getImports() {
        List<String> result = new ArrayList<>();

        result.addAll(parameterType.getImports());
        result.addAll(containerType.getImports());

        return result;
    }

    private static GenericInfo parseGeneric(String typeName, TypeResolver typeResolver) {
        return GenericInfo.parse(typeName, typeResolver)
                .orElseThrow(() -> new RuntimeException("Invalid generic type: " + typeName));
    }

    public Type getParameterType() {
        return parameterType;
    }

    public Type getContainerType() {
        return containerType;
    }

    public static boolean isGeneric(String typeName) {
        return GenericInfo.GENERIC_PATTERN.matcher(typeName).matches();
    }

    private static class GenericInfo {
        private final static Pattern GENERIC_PATTERN = Pattern.compile("^(\\w+)<(\\w+)>$");
        private final static Pattern GENERIC_PATTERN2 = Pattern.compile("^(\\w+)<\\? extends (\\w+)>$");
        private final Type containerType;
        private final Type parameterType;

        private GenericInfo(Type containerType, Type parameterType) {
            this.containerType = Validate.notNull(containerType, "containerType");
            ;
            this.parameterType = Validate.notNull(parameterType, "parameterType");
            ;
        }

        public static Optional<GenericInfo> parse(String typeName, TypeResolver typeResolver) {
            Matcher matcher = GenericInfo.GENERIC_PATTERN.matcher(typeName);
            boolean found = matcher.find();

            if (!found) {
                matcher = GenericInfo.GENERIC_PATTERN2.matcher(typeName);
                found = matcher.find();
            }

            if (found) {
                String genericContainer = matcher.group(1);
                String genericTypeStr = matcher.group(2);

                Type containerType = typeResolver.resolveByName(genericContainer);
                Type genericType = typeResolver.resolveByName(genericTypeStr);

                return Optional.of(new GenericInfo(containerType, genericType));
            }

            return Optional.empty();
        }
    }
}
