package io.tezrok.api.builder.expression;

import org.apache.commons.lang3.Validate;

public class JavaCondition extends JavaExpression {
    private final String condition;

    public JavaCondition(String condition) {
        this.condition = Validate.notNull(condition, "condition");
    }

    @Override
    public String toString() {
        return condition;
    }
}
