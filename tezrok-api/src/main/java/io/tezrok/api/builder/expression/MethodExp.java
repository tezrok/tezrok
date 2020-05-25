package io.tezrok.api.builder.expression;

import io.tezrok.api.builder.JavaField;
import io.tezrok.api.builder.JavaMethod;
import io.tezrok.api.builder.JavaVar;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Expression for method calling.
 * <p>
 * Expression for <code>field.fooBar(arg1, arg2)</code>.
 */
public class MethodExp extends JavaExpression {
    private final String fieldName;
    private final String methodName;
    private final List<String> params;

    public MethodExp(final JavaField field, final JavaMethod method, final List<String> params) {
        this(Validate.notNull(field).getName(),
                Validate.notNull(method, "method").getName(),
                Validate.notNull(params, "params")
                        .stream()
                        .collect(Collectors.toList()));
    }

    public MethodExp(final String fieldName, final String methodName, final List<String> params) {
        this.fieldName = fieldName;
        this.methodName = methodName;
        this.params = params;
    }

    public MethodExp(final JavaVar variable, final String methodName, final List<String> params) {
        this(variable.getName(), methodName, params);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append(fieldName);
        builder.append('.');
        builder.append(methodName);
        builder.append('(');

        if (!params.isEmpty()) {
            builder.append(params.stream()
                    .collect(joining(", ")));
        }

        builder.append(")");

        return builder.toString();
    }
}
