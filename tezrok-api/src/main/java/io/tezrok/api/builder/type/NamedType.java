package io.tezrok.api.builder.type;

import org.apache.commons.lang3.StringUtils;

public class NamedType extends Type {
    public NamedType(String name, String packagePath) {
        super(name, packagePath);
    }

    public NamedType(Class clazz) {
        this(clazz.getSimpleName(), getPackagePath(clazz));
    }

    private static String getPackagePath(Class clazz) {
        if (clazz.getPackage() != null) {
            return clazz.getPackage().getName();
        }

        return StringUtils.EMPTY;
    }
}
