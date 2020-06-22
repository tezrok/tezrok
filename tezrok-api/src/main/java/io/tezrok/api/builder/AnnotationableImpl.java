package io.tezrok.api.builder;

import io.tezrok.api.util.StringUtil;
import org.apache.commons.lang3.Validate;

import java.lang.annotation.Annotation;
import java.util.*;

public class AnnotationableImpl<TReturn> implements Annotationable<TReturn> {
    private final List<JavaAnnotation> javaAnnotations = new LinkedList<>();
    private final Set<String> imports = new HashSet<>();
    private final TReturn returnType;

    public AnnotationableImpl() {
        this(null);
    }

    public AnnotationableImpl(TReturn returnType) {
        this.returnType = returnType;
    }

    public TReturn annotate(Class<? extends Annotation> clazz) {
        return annotate(new JavaAnnotation(clazz));
    }

    public TReturn annotate(String name) {
        return annotate(new JavaAnnotation(name));
    }

    public TReturn annotate(JavaAnnotation annotation) {
        javaAnnotations.add(annotation);
        if (annotation.getAnnotationClass() != null) {
            addImportClass(annotation.getAnnotationClass().getName());
        }
        annotation.getAdditionalImports()
                .stream()
                .forEach(p -> addImportClass(p));

        return returnType != null ? returnType : (TReturn) this;
    }

    public List<JavaAnnotation> getAnnotations() {
        return javaAnnotations;
    }

    public boolean hasAnnotations() {
        return !javaAnnotations.isEmpty();
    }

    public Set<String> getImportClasses() {
        return imports;
    }

    @Override
    public TReturn addImports(String... imports) {
        for (String clazz : imports) {
            addImportClass(clazz);
        }

        return returnType != null ? returnType : (TReturn) this;
    }

    @Override
    public TReturn addImports(Class... imports) {
        for (Class clazz : imports) {
            addImportClass(clazz.getName());
        }

        return returnType != null ? returnType : (TReturn) this;
    }

    private void addImportClass(String importClass) {
        Validate.notBlank(importClass, "importClass");

        if (!"java.lang.Override".equals(importClass)) {
            imports.add(importClass);
        }
    }

    protected Set<String> mergeImportClasses(Collection<? extends AnnotationableImpl<?>> items) {
        final Set<String> classes = new HashSet(imports);

        items.stream().flatMap(p -> p.getImportClasses().stream()).forEach(classes::add);

        return Collections.unmodifiableSet(classes);
    }

    protected void renderAnnotations(StringBuilder result, int indent) {
        if (hasAnnotations()) {
            result.append(StringUtil.tab(indent));

            getAnnotations().stream()
                    .forEach(a -> result.append(a.toString()));

            result.append(StringUtil.NEWLINE);
        }
    }
}
