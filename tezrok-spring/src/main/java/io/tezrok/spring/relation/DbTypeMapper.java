package io.tezrok.spring.relation;


import io.tezrok.api.builder.type.EnumNodeType;
import io.tezrok.api.builder.type.PrimitiveType;
import io.tezrok.api.builder.type.Type;
import io.tezrok.api.error.TezrokException;
import io.tezrok.api.model.node.EnumNode;
import io.tezrok.api.model.node.FieldNode;

import java.util.HashMap;
import java.util.Map;

public class DbTypeMapper {
    private final static Map<String, String> simpleTypesMap = new HashMap<String, String>() {{
        put("void", "");
        put("Long", "bigint");
        put("long", "bigint");
        put("Boolean", "boolean");
        put("boolean", "boolean");
        put("Date", "timestamp");
        put("Integer", "int");
        put("int", "int");
    }};

    public static String mapSimpleDbType(FieldNode field, Type type) {
        if (type instanceof PrimitiveType) {
            return mapPrimitiveDbType(field.getType(), field);
        } else if (type instanceof EnumNodeType) {
            return String.format("varchar(%d)", EnumNode.NAME_MAX_SIZE);
        }

        throw new TezrokException("Unsupported type: " + type.getClass().getName());
    }

    public static String mapPrimitiveDbType(String name, FieldNode field) {
        String dbType = simpleTypesMap.get(name);

        if (dbType != null) {
            return dbType;
        }

        if (field != null) {
            if (name.equals("String")) {
                if (field.getMax() == null) {
                    throw new IllegalStateException(String.format("String field '%s.%s' must contain 'max' property",
                            field.getParent().getName(), field.getName()));
                }

                return String.format("varchar(%d)", field.getMax());
            }
        }

        throw new IllegalStateException("Simple db not found: " + name);
    }
}
