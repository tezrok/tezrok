package io.tezrok.api.builder.expression;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

public class SnippetExpression extends JavaExpression {
    private final String snippet;

    public SnippetExpression(@NotNull String snippet) {
        this.snippet = Validate.notBlank(snippet, "snippet");
    }

    @Override
    public String toString() {
        return snippet;
    }
}
