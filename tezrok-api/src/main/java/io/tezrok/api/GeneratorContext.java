package io.tezrok.api;

import io.tezrok.api.builder.Builder;
import io.tezrok.api.builder.type.Type;
import io.tezrok.api.model.node.ModuleNode;
import io.tezrok.api.model.node.ProjectNode;
import io.tezrok.api.visitor.MavenVisitor;

import java.util.List;

public interface GeneratorContext {
    boolean isGenerateTime();

    ProjectNode getProject();

    ModuleNode getModule();

    Type ofType(Class clazz);

    ModuleNode getModule(Type type);

    <T> T getInstance(Class<T> clazz);

    List<MavenVisitor> getMavenVisitors();

    void render(Builder builder);
}
