package io.tezrok.api.builder.expression;

import io.tezrok.api.builder.JavaField;
import org.apache.commons.lang3.Validate;

public class FieldAssignExpression extends JavaExpression {
    private final JavaField field;
    private final String expressionValue;

    public FieldAssignExpression(JavaField field, JavaExpression expression) {
        this(field, expression.toString());
    }

    public FieldAssignExpression(JavaField field, String expressionValue) {
        this.field = Validate.notNull(field, "field");
        this.expressionValue = Validate.notBlank(expressionValue, "expressionValue");
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("this.");

        result.append(field.getName());
        result.append(" = ");
        result.append(expressionValue);
        result.append(';');

        return result.toString();
    }
}
