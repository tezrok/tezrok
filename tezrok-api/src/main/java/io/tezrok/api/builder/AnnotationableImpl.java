package io.tezrok.api.builder;

import io.tezrok.api.util.StringUtil;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotationableImpl<TReturn> implements Annotationable<TReturn> {
    private final List<JavaAnnotation> javaAnnotations = new LinkedList<>();
    private final Set<String> importClasses = new HashSet<>();
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
            addAnnotationClass(annotation.getAnnotationClass().getName());
        }
        annotation.getAdditionalImports()
                .stream()
                .forEach(p -> addAnnotationClass(p));

        return returnType != null ? returnType : (TReturn) this;
    }

    public List<JavaAnnotation> getAnnotations() {
        return javaAnnotations;
    }

    public boolean hasAnnotations() {
        return !javaAnnotations.isEmpty();
    }

    public Set<String> getImportClasses() {
        return importClasses;
    }

    private void addAnnotationClass(String clazz) {
        if (clazz != null && !importClasses.contains(clazz)) {
            importClasses.add(clazz);
        }
    }

    protected Set<String> mergeImportClasses(Collection<? extends AnnotationableImpl<?>> items) {
        final Set<String> classes = importClasses
                .stream()
                .collect(Collectors.toSet());

        for (AnnotationableImpl<?> param : items) {
            for (String clazz : param.getImportClasses()) {
                if (!classes.contains(clazz)) {
                    classes.add(clazz);
                }
            }
        }

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
