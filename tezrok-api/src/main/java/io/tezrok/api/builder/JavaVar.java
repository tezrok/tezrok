package io.tezrok.api.builder;

import io.tezrok.api.builder.type.Type;
import org.apache.commons.lang3.Validate;

public class JavaVar {
    private final String name;
    private final Type type;

    private JavaVar(final String name, final Type type) {
        this.name = Validate.notBlank(name);
        this.type = Validate.notNull(type);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public static JavaVar create(final String name, final Type type){
        return new JavaVar(name, type);
    }
}
