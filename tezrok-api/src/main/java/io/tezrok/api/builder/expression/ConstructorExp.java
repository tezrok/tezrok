package io.tezrok.api.builder.expression;

import io.tezrok.api.builder.JavaMethodParam;
import io.tezrok.api.builder.type.Type;
import org.apache.commons.lang3.Validate;

import java.util.List;

import static java.util.stream.Collectors.joining;

public class ConstructorExp extends  JavaExpression {
    private final List<JavaMethodParam> params;
    private final Type type;

    public ConstructorExp(Type type, List<JavaMethodParam> params) {
        this.type = Validate.notNull(type);
        this.params = Validate.notNull(params);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("new ");
        builder.append(type.getName());
        builder.append('(');

        if (!params.isEmpty()){
            builder.append(params.stream()
                    .map(p -> p.getName())
                    .collect(joining(", ")));
        }

        builder.append(")");

        return builder.toString();
    }
}
