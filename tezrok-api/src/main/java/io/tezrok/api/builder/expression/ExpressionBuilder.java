package io.tezrok.api.builder.expression;


import io.tezrok.api.builder.*;
import io.tezrok.api.builder.type.Type;

import java.util.List;

public class ExpressionBuilder {
    public static IfExp _if(String condition) {
        return _if(new JavaCondition(condition));
    }

    public static IfExp _if(JavaCondition condition) {
        return new IfExp(condition);
    }

    public static MethodExp _method(final JavaField field, final JavaMethod method, final List<String> params) {
        return new MethodExp(field, method, params);
    }

    public static MethodExp _method(final String fieldName, final String methodName, final List<String> params) {
        return new MethodExp(fieldName, methodName, params);
    }

    public static MethodExp _method(final JavaVar variable, final String methodName, final List<String> params) {
        return new MethodExp(variable, methodName, params);
    }

    public static VarExp _var(JavaVar variable) {
        return new VarExp(variable);
    }

    public static BlockExp _block() {
        return new BlockExp();
    }

    public static NewLineExp _newLine() {
        return new NewLineExp();
    }

    public static ReturnExp _return(JavaExpression expression) {
        return new ReturnExp(expression);
    }

    public static ReturnExp _return(String expression) {
        return new ReturnExp(expression);
    }

    public static ConstructorExp _constructor(final Type type, final List<JavaMethodParam> params) {
        return new ConstructorExp(type, params);
    }

    public static SnippetExpression _snippet(String snippet) {
        return new SnippetExpression(snippet);
    }

    public static JavaConstructor initFieldsInConstructor(JavaConstructor constructor, JavaField... fields) {
        BlockExp body = new BlockExp();

        for (JavaField field : fields) {
            JavaMethodParam param = constructor.param(field.getName(), field.getType());
            body.add(new FieldAssignExpression(field, param.getName()));
        }

        constructor.setBody(body);

        return constructor;
    }
}
