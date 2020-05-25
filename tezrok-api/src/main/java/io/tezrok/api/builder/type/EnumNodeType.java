package io.tezrok.api.builder.type;

import io.tezrok.api.model.node.EnumNode;

public class EnumNodeType extends Type {
    private final EnumNode enumNode;

    public EnumNodeType(EnumNode enumNode, String packagePath) {
        super(enumNode.getName(), packagePath);
        this.enumNode = enumNode;
    }

    @Override
    public boolean isEnum() {
        return true;
    }

    public EnumNode getEnumNode() {
        return enumNode;
    }
}
