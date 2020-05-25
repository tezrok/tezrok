package io.tezrok.api.builder;

import io.tezrok.api.builder.type.Type;
import org.apache.commons.lang3.Validate;

public class JavaMethodParam extends AnnotationableImpl<JavaMethodParam> {
    private final String name;
    private final Type type;
    private final JavaMethod method;

    public JavaMethodParam(final String name, final Type type, final JavaMethod method) {
        this.name = NameUtil.validate(name);
        this.type = Validate.notNull(type, "type");
        this.method = Validate.notNull(method, "method");
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public JavaMethod and() {
        return method;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        getAnnotations().stream()
                .forEach(a ->
                {
                    result.append(a.toString());
                    result.append(" ");
                });
        result.append(getType().getName());
        result.append(" ");
        result.append(getName());

        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavaMethodParam rhs = (JavaMethodParam) o;

        if (!name.equals(rhs.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + method.hashCode();
        return result;
    }
}
