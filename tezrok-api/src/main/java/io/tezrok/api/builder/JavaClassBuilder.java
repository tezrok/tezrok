package io.tezrok.api.builder;

import io.tezrok.api.ExecuteContext;
import io.tezrok.api.builder.expression.FieldAssignExpression;
import io.tezrok.api.builder.expression.ReturnExp;
import io.tezrok.api.builder.type.PrimitiveType;
import io.tezrok.api.builder.type.Type;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.stream.Collectors.toList;

public abstract class JavaClassBuilder extends VelocityBuilder implements Annotationable<JavaClassBuilder> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final String MAIN_PATH = "src/main/java/";

    private final Type type;
    private final JMod mod;
    private final Set<JavaField> fields = new LinkedHashSet<>();
    private final Set<String> interfaces = new LinkedHashSet<>();
    private final Set<String> imports = new LinkedHashSet<>();
    private final Set<JavaMethod> methods = new LinkedHashSet<>();
    private final Set<JavaConstructor> constructors = new LinkedHashSet<>();
    private String equalsField;
    private String comment;
    private String superClass;
    private boolean hasToString = false;
    private boolean customCode = false;
    private final AnnotationableImpl<JavaClassBuilder> annotationable;

    protected JavaClassBuilder(Type type, int mod, final ExecuteContext context) {
        super(context);
        this.type = Validate.notNull(type, "type");
        this.mod = JMod.valueOf(mod);
        annotationable = new AnnotationableImpl<>(this);
    }

    @Override
    protected void onBuild(VelocityContext context) {
        addImportsFromAnnotated();
        addImportsFromFieldsAndMethods();

        List<String> importLines = imports.stream().collect(toList());
        Collections.sort(importLines);

        context.put("package", type.getPackage());
        context.put("name", type.getName());
        context.put("comment", comment);
        context.put("imports", importLines);
        context.put("fields", fields);
        context.put("annotations", getAnnotations());
        context.put("modificator", getModificator());
        context.put("kind", getKind());
        context.put("inheritance", getInheritance());
        context.put("hasToString", !mod.isInterface() && hasToString);
        context.put("hasMethods", hasMethods());
        context.put("methods", methods);
        context.put("constructors", constructors);

        if (StringUtils.isNotBlank(equalsField)) {
            context.put("equalsField", equalsField);
        }
    }

    private String getKind() {
        return mod.isInterface() ? "interface" : "class";
    }

    private String getModificator() {
        StringBuilder result = new StringBuilder();

        if (mod.isPublic()) {
            result.append("public ");
        }

        if (mod.isAbstract()) {
            return "abstract ";
        }

        if (mod.isFinal()) {
            result.append("final ");
        }

        return result.toString();
    }

    private String getInheritance() {
        StringBuilder inheritance = new StringBuilder();

        if (StringUtils.isNotBlank(superClass)) {
            inheritance.append("extends ");
            inheritance.append(superClass);
            inheritance.append(" ");
        }

        if (!interfaces.isEmpty()) {
            inheritance.append("implements ");

            for (String iface : interfaces) {
                inheritance.append(iface);
                inheritance.append(" ");
            }
        }

        return inheritance.toString();
    }

    public JavaField field(String name, Type type) {
        return field(name, type, JMod.PRIVATE | JMod.GETSET);
    }

    public JavaField field(String name, Class<?> type) {
        return field(name, type, JMod.PRIVATE | JMod.GETSET);
    }

    public JavaField field(String name, Class<?> type, int mod) {
        return field(name, getContext().ofType(type), mod);
    }

    public JavaField field(String name, Type type, int mod) {
        JavaField field = new JavaField(name, type, mod, this);
        fields.add(field);

        if (field.isUsedInEquals()) {
            equalsField = field.getName();
        }

        if (field.hasGet()) {
            JavaMethod method = method(field.getGetName(), field.getType(), JMod.PUBLIC | JMod.GET);
            method.setBody(new ReturnExp(field.getName()));
        }

        if (field.hasSet()) {
            JavaMethod method = method(field.getSetName(), PrimitiveType.VOID, JMod.PUBLIC | JMod.SET);
            JavaMethodParam param = method.param(name, type);
            method.setBody(new FieldAssignExpression(field, param.getName()));
        }

        return field;
    }

    /**
     * Gets field by type.
     */
    public JavaField getField(JavaClassBuilder clazz) {
        Optional<JavaField> field = fields.stream()
                .filter(p -> p.getType().getName().equals(clazz.getName()))
                .findFirst();

        if (field.isPresent()) {
            return field.get();
        }

        throw new IllegalStateException("Field not found by type: " + clazz.getType());
    }

    public Optional<JavaField> getField(String name) {
        return fields.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();
    }

    public boolean hasField(String name) {
        return getField(name).isPresent();
    }

    public JavaClassBuilder comment(String comment) {
        this.comment = comment;

        return this;
    }

    public JavaClassBuilder addImplements(String... names) {
        interfaces.addAll(Arrays.asList(names));

        return this;
    }

    public JavaClassBuilder addImplements(Class... classes) {
        for (Class clazz : classes) {
            interfaces.add(clazz.getSimpleName());
            addImportClass(clazz.getName());
        }

        return this;
    }

    public JavaClassBuilder setExtends(Class clazz) {
        setExtends(clazz.getSimpleName());
        addImportClass(clazz.getName());

        return this;
    }

    public JavaClassBuilder setExtends(String superClass) {
        if (StringUtils.isNotBlank(this.superClass)) {
            throw new IllegalStateException("Super class already defined as " + this.superClass);
        }
        this.superClass = superClass;

        return this;
    }

    public JavaClassBuilder setExtends(Type type) {
        setExtends(type.getName());
        addImports(type.getFullName());
        addImportsEx(type.getImports());

        return this;
    }

    @Override
    public JavaClassBuilder addImports(String... imports) {
        return addImportsEx(Arrays.asList(imports));
    }

    @Override
    public JavaClassBuilder addImports(Class... imports) {
        return addImports(Arrays.asList(imports));
    }

    public JavaClassBuilder addImportsEx(List<String> imports) {
        for (String importClass : imports) {
            addImportClass(importClass);
        }

        return this;
    }

    public JavaClassBuilder addImports(List<Class> imports) {
        for (Class clazz : imports) {
            addImportClass(clazz.getName());
        }

        return this;
    }

    public String getPackage() {
        return type.getPackage();
    }

    public String getFullName() {
        return type.getFullName();
    }

    public String getName() {
        return type.getName();
    }

    @Override
    public String getPath() {
        return MAIN_PATH + getPackage().replace('.', '/');
    }

    @Override
    public String getFileName() {
        return getName() + ".java";
    }

    public boolean isHasToString() {
        return hasToString;
    }

    public boolean isAbstract() {
        return mod.isAbstract();
    }

    public boolean isPublic() {
        return mod.isPublic();
    }

    public boolean isInterface() {
        return mod.isInterface();
    }

    /**
     * If <code>customCode</code> is <code>true</code> and class already exists it won't be generated.
     */
    public JavaClassBuilder setCustomCode(boolean customCode) {
        this.customCode = customCode;
        return this;
    }

    @Override
    public boolean isCustomCode() {
        return customCode;
    }

    public JavaClassBuilder setHasToString(boolean hasToString) {
        this.hasToString = hasToString;
        return this;
    }

    public JavaMethod method(final String name, final Type type) {
        return method(name, type, JMod.PUBLIC);
    }

    public JavaMethod method(final String name, final Type type, final int mod) {
        final JavaMethod method = new JavaMethod(name, type, mod, this);

        methods.add(method);

        // if added method is abstract class must be abstract too
        if (JMod.valueOf(mod).isAbstract()) {
            this.mod.makeAbstract();
        }

        return method;
    }

    public JavaConstructor constructor() {
        return constructor(JMod.PUBLIC);
    }

    public JavaConstructor constructor(final int mod) {
        final JavaConstructor constructor = new JavaConstructor(this, mod);

        constructors.add(constructor);

        return constructor;
    }

    public boolean hasMethods() {
        return !methods.isEmpty();
    }

    public Optional<JavaMethod> findMethod(final String name) {
        return methods.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();
    }

    @Override
    public String toString() {
        return "CLASS: " + getName();
    }

    private void addImportClass(String importClass) {
        Validate.notBlank(importClass, "importClass");

        if (!"java.lang.Override".equals(importClass)) {
            imports.add(importClass);
        }
    }

    private void addImportsFromAnnotated() {
        for (String clazz : getImportClasses()) {
            addImportClass(clazz);
        }

        addImportsFromAnnotated(fields);
        addImportsFromAnnotated(methods);
    }

    private void addImportsFromAnnotated(Set<? extends Annotationable<?>> items) {
        for (Annotationable<?> item : items) {
            for (String clazz : item.getImportClasses()) {
                addImportClass(clazz);
            }
        }
    }

    private void addImportsFromFieldsAndMethods() {
        for (JavaField field : fields) {
            for (String clazz : field.getType().getImports()) {
                addImportClass(clazz);
            }
        }

        for (JavaMethod method : methods) {
            for (String clazz : method.getType().getImports()) {
                addImportClass(clazz);
            }
        }
    }

    public JavaMethod methodMain() {
        JavaMethod method = method("main", PrimitiveType.VOID, JMod.PUBLIC | JMod.STATIC);
        method.param("args", getContext().ofType(String[].class));

        return method;
    }

    public Type getType() {
        return type;
    }

    @Override
    public JavaClassBuilder annotate(Class<? extends Annotation> clazz) {
        return annotationable.annotate(clazz);
    }

    @Override
    public JavaClassBuilder annotate(String name) {
        return annotationable.annotate(name);
    }

    @Override
    public JavaClassBuilder annotate(JavaAnnotation annotation) {
        return annotationable.annotate(annotation);
    }

    @Override
    public List<JavaAnnotation> getAnnotations() {
        return annotationable.getAnnotations();
    }

    @Override
    public boolean hasAnnotations() {
        return annotationable.hasAnnotations();
    }

    @Override
    public Set<String> getImportClasses() {
        return annotationable.getImportClasses();
    }
}
