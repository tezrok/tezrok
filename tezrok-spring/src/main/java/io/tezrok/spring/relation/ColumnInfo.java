package io.tezrok.spring.relation;

import io.tezrok.api.model.node.FieldNode;
import org.apache.commons.lang3.Validate;

public abstract class ColumnInfo {
    private final FieldNode field;

    public ColumnInfo(FieldNode field) {
        this.field = Validate.notNull(field, "field");
    }

    public FieldNode getField() {
        return field;
    }

    public boolean isBasic() {
        return getClass().equals(BasicColumnInfo.class);
    }

    public boolean isOneToMany() {
        return getClass().equals(OneToManyColumnInfo.class);
    }

    public boolean isOneToOne() {
        return getClass().equals(OneToOneColumnInfo.class);
    }

    public boolean isManyToMany() {
        return getClass().equals(ManyToManyColumnInfo.class);
    }

    public boolean isManyToOne() {
        return getClass().equals(ManyToOneColumnInfo.class);
    }

    public <T extends ColumnInfo> T as() {
        return (T) this;
    }
}
