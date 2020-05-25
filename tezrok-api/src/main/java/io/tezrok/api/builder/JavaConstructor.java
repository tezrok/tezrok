package io.tezrok.api.builder;

import io.tezrok.api.builder.type.PrimitiveType;
import org.apache.commons.lang3.StringUtils;

public class JavaConstructor extends JavaMethod {
    public JavaConstructor(JavaClassBuilder ownerClass, int mod) {
        super(ownerClass.getName(), PrimitiveType.VOID, JMod.CONSTRUCTOR | mod, ownerClass);
        setBody(StringUtils.EMPTY);
    }
}
