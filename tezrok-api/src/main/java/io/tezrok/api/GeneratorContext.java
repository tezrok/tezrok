package io.tezrok.api;

import io.tezrok.api.builder.type.Type;
import io.tezrok.api.model.node.ModuleNode;
import io.tezrok.api.model.node.ProjectNode;

public interface GeneratorContext {
    boolean isGenerateTime();

    ProjectNode getProject();

    Type ofType(Class clazz);

    ModuleNode getModule(Type type);

    <T> T getInstance(Class<T> clazz);
}
