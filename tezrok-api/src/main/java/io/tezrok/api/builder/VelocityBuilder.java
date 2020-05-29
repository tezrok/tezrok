package io.tezrok.api.builder;

import io.tezrok.api.ExecuteContext;
import io.tezrok.api.model.node.ProjectNode;
import org.apache.commons.lang3.Validate;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.io.Writer;
import java.time.OffsetDateTime;

/**
 * Builder based on Velocity template
 */
public abstract class VelocityBuilder implements Builder {
    private final ExecuteContext context;

    protected VelocityBuilder(ExecuteContext context) {
        this.context = Validate.notNull(context, "context");
    }

    @Override
    public void build(final Writer writer) {
        VelocityContext context = new VelocityContext();

        if (getContext().isGenerateTime()) {
            context.put("generateTime", OffsetDateTime.now().toString());
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

    public ExecuteContext getContext() {
        return context;
    }

    public ProjectNode getProject() {
        return getContext().getProject();
    }
}
