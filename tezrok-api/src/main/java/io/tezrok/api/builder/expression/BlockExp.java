package io.tezrok.api.builder.expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockExp extends JavaExpression {
    private final List<JavaExpression> expressions = new ArrayList<>();

    public BlockExp add(JavaExpression expression) {
        expressions.add(expression);

        return this;
    }

    public BlockExp addAll(JavaExpression... expression) {
        expressions.addAll(Arrays.asList(expression));
        return this;
    }

    public void clear() {
        expressions.clear();
    }

    @Override
    public boolean isEmpty() {
        return expressions.isEmpty() || expressions.stream().anyMatch(p -> p != EMPTY);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (JavaExpression exp : expressions) {
            if (result.length() > 0) {
                result.append("\n\t\t");
            }

            if (exp != JavaExpression.NEWLINE) {
                result.append(exp.toString().replaceAll("\\n", "\n\t\t"));
            }
        }

        return result.toString();
    }

    public static BlockExp asList(JavaExpression... expressions) {
        return new BlockExp().addAll(expressions);
    }
}
