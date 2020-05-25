package io.tezrok.api.builder.expression;

import org.apache.commons.lang3.Validate;

public class IfExp extends JavaExpression {
    private final JavaCondition condition;
    private JavaExpression thenExpression;
    private JavaExpression elseExpression;
    private IfExp elseIfExpression;

    public IfExp(JavaCondition condition) {
        this.condition = Validate.notNull(condition);
    }

    public IfExp _then(JavaExpression exp){
        thenExpression = Validate.notNull(exp);
        return this;
    }

    public void _else(JavaExpression exp){
        elseExpression = Validate.notNull(exp);
    }

    public IfExp _elseif(JavaCondition condition){
        elseIfExpression = new IfExp(condition);

        return elseIfExpression;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("if (");
        sb.append(condition.toString());
        sb.append(") {\n\t");
        sb.append(thenExpression.toString());
        sb.append("\n}");

        if (elseExpression != null) {
            sb.append(" else {\n\t");
            sb.append(elseExpression.toString());
            sb.append("\n}");
        } else if (elseIfExpression != null) {
            sb.append(" else ");
            sb.append(elseIfExpression.toString());
        }

        return sb.toString();
    }
}
