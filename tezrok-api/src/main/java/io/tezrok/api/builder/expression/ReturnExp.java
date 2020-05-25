package io.tezrok.api.builder.expression;

public class ReturnExp extends JavaExpression {
    private final String expression;

    public ReturnExp(JavaExpression expression) {
        this(expression.toString());
    }

    public ReturnExp(String expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("return ");

        result.append(expression.toString());
        result.append(';');

        return result.toString();
    }
}
