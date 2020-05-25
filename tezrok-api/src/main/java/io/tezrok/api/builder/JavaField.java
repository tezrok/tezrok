package io.tezrok.api.builder;

import io.tezrok.api.builder.type.PrimitiveType;
import io.tezrok.api.builder.type.Type;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.text.WordUtils;

/**
 * Java field representation.
 */
public class JavaField extends AnnotationableImpl<JavaField> {
    private final String name;
    private final Type type;
    private final JMod mod;
    private final JavaClassBuilder clazz;
    private String value;

    public JavaField(final String name, final Type type, final int mod, final JavaClassBuilder clazz) {
        this.name = NameUtil.validate(name);
        this.type = Validate.notNull(type, "type");
        this.mod = JMod.valueOf(mod);
        this.clazz = Validate.notNull(clazz, "clazz");
    }

    public JavaClassBuilder getOwnerClass() {
        return clazz;
    }

    public JavaClassBuilder and() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public boolean isUsedInEquals() {
        return mod.isUsedInEquals();
    }

    public String getCapName() {
        return WordUtils.capitalize(name);
    }

    public boolean hasGet() {
        return mod.isGet();
    }

    public boolean hasSet() {
        return mod.isSet();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getGetName() {
        if (type.isPrimitive()) {
            PrimitiveType pt = type.as();

            if (pt.equals(PrimitiveType.BOOLOBJ)) {
                return "is" + getCapName();
            }
        }

        return "get" + getCapName();
    }

    public String getSetName() {
        return "set" + getCapName();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        if (mod.isPrivate()) {
            result.append("private ");
        } else if (mod.isProtected()) {
            result.append("protected ");
        } else if (mod.isPublic()) {
            result.append("public ");
        }

        if (mod.isStatic()) {
            result.append("static ");
        }

        if (mod.isFinal()) {
            result.append("final ");
        }

        result.append(type.getName());
        result.append(" ");
        result.append(name);

        if (StringUtils.isNotBlank(value)) {
            result.append(" = ");
            result.append(value);
        }

        result.append(";");

        return result.toString();
    }
}
