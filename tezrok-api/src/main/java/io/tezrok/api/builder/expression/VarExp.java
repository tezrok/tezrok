package io.tezrok.api.builder.expression;

import io.tezrok.api.builder.JavaMethodParam;
import io.tezrok.api.builder.JavaVar;
import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.List;

public class VarExp extends JavaExpression {
    private final JavaVar variable;
    private JavaExpression rightExpression;

    public VarExp(JavaVar variable) {
        this.variable = Validate.notNull(variable);
    }

    public VarExp assign(JavaExpression rightExpression) {
        this.rightExpression = rightExpression;

        return this;
    }

    public VarExp assignConstructor(final List<JavaMethodParam> params){
        this.assign(ExpressionBuilder._constructor(variable.getType(), params));

        return this;
    }

    public VarExp assignConstructor(){
        return this.assignConstructor(Collections.emptyList());
    }

    public MethodExp callMethod(final String methodName, final List<String> params){
        return ExpressionBuilder._method(variable, methodName, params);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(variable.getType().getName());
        builder.append(' ');
        builder.append(variable.getName());

        if (rightExpression != null) {
            builder.append(" = ");
            builder.append(rightExpression.toString());
        }

        builder.append(';');

        return builder.toString();
    }
}
