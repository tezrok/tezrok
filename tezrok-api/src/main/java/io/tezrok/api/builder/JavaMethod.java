package io.tezrok.api.builder;

import io.tezrok.api.builder.expression.JavaExpression;
import io.tezrok.api.builder.type.NamedType;
import io.tezrok.api.builder.type.Type;
import io.tezrok.api.util.StringUtil;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class JavaMethod extends AnnotationableImpl<JavaMethod> {
    private final String name;
    private final JavaClassBuilder ownerClass;
    private final JMod mod;
    private final Type type;
    private final List<JavaMethodParam> params = new ArrayList<>();
    private final List<Type> throwsList = new ArrayList<>();
    private JavaExpression body;
    private int indent = 1;

    public JavaMethod(@NotNull final String name, final Type type, int mod,
                      @NotNull final JavaClassBuilder ownerClass) {
        this.name = NameUtil.validate(name);
        this.type = Validate.notNull(type, "type");
        this.ownerClass = Validate.notNull(ownerClass, "ownerClass");
        this.mod = JMod.valueOf(mod);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public List<JavaMethodParam> getParams() {
        return params;
    }

    public List<String> getStringParams() {
        return getParams().stream()
                .map(p -> p.getName())
                .collect(toList());
    }

    public JavaClassBuilder getOwnerClass() {
        return ownerClass;
    }

    public JavaExpression getBody() {
        return body;
    }

    public JavaMethod setBody(JavaExpression expression) {
        if (expression != null) {
            if (mod.isAbstract()) {
                throw new IllegalStateException("Abstract method cannot have a body");
            }

            if (ownerClass.isInterface()) {
                throw new IllegalStateException("Interface method cannot have a body");
            }
        }

        this.body = expression;

        return this;
    }

    public boolean hasBody() {
        return body != null;
    }

    public JavaMethodParam param(final String name, final Type type) {
        JavaMethodParam param = new JavaMethodParam(name, type, this);

        params.add(param);

        return param;
    }

    @Override
    public Set<String> getImportClasses() {
        Set<String> imports = new HashSet<>(mergeImportClasses(params));
        throwsList.stream()
                .map(p -> p.getFullName())
                .forEach(imports::add);

        return imports;
    }

    public JavaMethod addThrows(Class clazz) {
        return addThrows(new NamedType(clazz));
    }

    public JavaMethod addThrows(Type type) {
        if (!throwsList.contains(type)) {
            throwsList.add(type);
        }

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavaMethod rhs = (JavaMethod) o;

        if (!ownerClass.equals(rhs.ownerClass)) return false;
        if (!name.equals(rhs.name)) return false;
        if (params.size() != rhs.getParams().size()) return false;

        for (int i = 0; i < params.size(); i++) {
            if (!params.get(i).equals(rhs.getParams().get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + ownerClass.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        renderAnnotations(result, indent);

        result.append(StringUtil.tab(indent));

        if (mod.isPrivate()) {
            result.append("private ");
        } else if (mod.isProtected()) {
            result.append("protected ");
        } else if (mod.isPublic()) {
            result.append("public ");
        }

        if (mod.isAbstract()) {
            result.append("abstract ");
        }

        if (mod.isStatic()) {
            result.append("static ");
        }

        if (!mod.isConstructor()) {
            result.append(getType().getName());
            result.append(" ");
        }
        result.append(getName());
        result.append("(");
        result.append(paramsToString());
        result.append(")");

        if (!throwsList.isEmpty()) {
            result.append(" throws");
            boolean first = true;

            for (Type type : throwsList) {
                if (!first) {
                    result.append(',');
                }
                result.append(' ');
                result.append(type.getName());
                first = false;
            }
        }

        if (hasBody()) {
            result.append(" {");
            result.append(StringUtil.NEWLINE);
            result.append(StringUtil.tab(indent + 1));
            result.append(getBody());
            result.append(StringUtil.NEWLINE);
            result.append(StringUtil.tab(indent));
            result.append("}");
        } else {
            result.append(";");
        }

        return result.toString();
    }

    private String paramsToString() {
        return params.stream()
                .map(p -> p.toString())
                .collect(Collectors.joining(", "));
    }
}
