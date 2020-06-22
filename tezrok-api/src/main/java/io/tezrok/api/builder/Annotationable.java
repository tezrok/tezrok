package io.tezrok.api.builder;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public interface Annotationable<TReturn> {
    TReturn annotate(Class<? extends Annotation> clazz);

    TReturn annotate(String name);

    TReturn annotate(JavaAnnotation annotation) ;

    List<JavaAnnotation> getAnnotations();

    boolean hasAnnotations();

    Set<String> getImportClasses();

    TReturn addImports(String... imports);

    TReturn addImports(Class... imports);
}
