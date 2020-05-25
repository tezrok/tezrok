package io.tezrok.api.builder.expression;

import org.apache.commons.lang3.Validate;

public class SemicolonExp extends JavaExpression {
    private final JavaExpression expression;
    private final boolean newLine;

    public SemicolonExp(final JavaExpression expression) {
        this(expression, false);
    }


        public SemicolonExp(final JavaExpression expression, boolean newLine) {
        this.newLine = newLine;
        this.expression = Validate.notNull(expression);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(expression.toString());
        result.append(";");

        if (newLine){
            result.append("\n");
        }

        return result.toString();
    }
}
