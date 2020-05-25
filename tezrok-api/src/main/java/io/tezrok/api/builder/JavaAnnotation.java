package io.tezrok.api.builder;

import org.apache.commons.lang3.Validate;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class JavaAnnotation {
    private final String name;
    private final Class<? extends Annotation> clazz;

    public JavaAnnotation(final String name, final Class<? extends Annotation> clazz) {
        this.name = Validate.notBlank(name, "name");
        this.clazz = clazz;
    }

    public JavaAnnotation(String name) {
        this(name, null);
    }

    public JavaAnnotation(Class<? extends Annotation> clazz) {
        this(clazz.getSimpleName(), clazz);
    }

    public String getName() {
        return name;
    }

    public Class<? extends Annotation> getAnnotationClass() {
        return clazz;
    }

    public List<String> getAdditionalImports() {
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return "@" + name;
    }
}
