package io.tezrok.api.builder;

import io.tezrok.api.builder.expression.JavaExpression;
import io.tezrok.api.builder.type.PrimitiveType;

public class JavaConstructor extends JavaMethod {
    public JavaConstructor(JavaClassBuilder ownerClass, int mod) {
        super(ownerClass.getName(), PrimitiveType.VOID, JMod.CONSTRUCTOR | mod, ownerClass);
        setBody(JavaExpression.EMPTY);
    }
}
