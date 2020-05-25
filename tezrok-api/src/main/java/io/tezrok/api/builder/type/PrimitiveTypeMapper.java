package io.tezrok.api.builder.type;

import java.util.HashMap;
import java.util.Map;

public class PrimitiveTypeMapper {
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

    public static boolean isPrimitive(String typeName) {
        if (simpleTypesMap.containsKey(typeName)) {
            return true;
        }

        if (typeName.equals("String")) {
            return true;
        }

        return false;
    }
}
