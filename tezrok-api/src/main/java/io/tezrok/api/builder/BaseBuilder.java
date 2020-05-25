package io.tezrok.api.builder;

import io.tezrok.api.GeneratorContext;
import io.tezrok.api.model.node.ModuleNode;
import io.tezrok.api.model.node.ProjectNode;
import org.apache.commons.lang3.Validate;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class BaseBuilder implements Builder {
    private final GeneratorContext generatorContext;
    private final ModuleNode module;

    protected BaseBuilder(ModuleNode module, GeneratorContext generatorContext) {
        this.generatorContext = Validate.notNull(generatorContext, "generatorContext");
        this.module = Validate.notNull(module, "module");
    }

    @Override
    public void build(final Writer writer) {
        VelocityContext context = new VelocityContext();

        if (getContext().isGenerateTime()) {
            context.put("generateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        onBuild(context);

        getTemplate().merge(context, writer);
    }

    protected abstract void onBuild(VelocityContext context);

    protected abstract Template getTemplate();

    public void init() {
    }

    @Override
    public boolean isCustomCode() {
        return false;
    }

    public GeneratorContext getContext() {
        return generatorContext;
    }

    public ProjectNode getProject() {
        return getContext().getProject();
    }
}
