package io.tezrok.api.builder.type;

import java.util.HashSet;
import java.util.Set;

public class PrimitiveTypeHelper {
    private final static Set<String> simpleTypes = new HashSet<String>() {{
        add("void");
        add("Long");
        add("long");
        add("Boolean");
        add("boolean");
        add("Date");
        add("Integer");
        add("int");
    }};

    public static boolean isPrimitive(String typeName) {
        if (simpleTypes.contains(typeName)) {
            return true;
        }

        if (typeName.equals("String")) {
            return true;
        }

        return false;
    }
}
