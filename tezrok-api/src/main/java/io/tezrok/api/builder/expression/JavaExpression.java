package io.tezrok.api.builder.expression;

import io.tezrok.api.util.StringUtil;

/**
 * Base class for all java expressions.
 */
public abstract class JavaExpression {
    public final static JavaExpression NEWLINE = new JavaExpression() {
        @Override
        public String toString() {
            return StringUtil.NEWLINE;
        }
    };

    public final static JavaExpression EMPTY = new JavaExpression() {
        @Override
        public String toString() {
            return "";
        }
    };

    public boolean isEmpty() {
        return this == EMPTY;
    }

    @Override
    public String toString() {
        throw new IllegalStateException("Subclass must implement this method");
    }
}
