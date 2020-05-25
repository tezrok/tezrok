package io.tezrok.api.builder.type;

import io.tezrok.api.model.node.Node;
import org.apache.commons.lang3.Validate;

public class NamedNodeType extends Type {
    private final Node node;

    public NamedNodeType(Node node, String packagePath) {
        super(Validate.notNull(node).getName(), packagePath);
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    @Override
    public boolean isEntity() {
        return node instanceof Node;
    }
}
