package io.tezrok.api.builder.type;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Representation of primitive types.
 */
public class PrimitiveType extends Type {
    public static final Type VOID = new PrimitiveType("void");
    public static final PrimitiveType STRING = new PrimitiveType("String");
    public static final PrimitiveType BOOLOBJ = new PrimitiveType("Boolean");
    public static final PrimitiveType BOOL = new PrimitiveType("boolean");
    public static final PrimitiveType LONGOBJ = new PrimitiveType("Long");
    public static final PrimitiveType LONG = new PrimitiveType("long");
    public static final PrimitiveType INT = new PrimitiveType("int");
    public static final PrimitiveType INTEGER = new PrimitiveType("Integer");

    public PrimitiveType(String name) {
        super(name, StringUtils.EMPTY);

        if (!isPrimitive(name)) {
            throw new RuntimeException("Type is not primitive: " + name);
        }
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public List<String> getImports() {
        return Collections.emptyList();
    }

    public static boolean isPrimitive(String typeName) {
        return PrimitiveTypeMapper.isPrimitive(typeName);
    }
}
