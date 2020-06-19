package io.tezrok.spring.relation;

import io.tezrok.api.model.node.FieldNode;
import org.apache.commons.lang3.Validate;

import javax.persistence.FetchType;

public class ManyToOneColumnInfo extends ColumnInfo {
    private final JoinColumn joinColumn;
    private final FetchType fetch;
    private final boolean optional;

    public ManyToOneColumnInfo(FieldNode field, JoinColumn joinColumn, FetchType fetch, boolean optional) {
        super(field);
        this.fetch = fetch;
        this.optional = optional;
        this.joinColumn = Validate.notNull(joinColumn, "joinColumn");
    }

    public JoinColumn getJoinColumn() {
        return joinColumn;
    }

    public FetchType getFetch() {
        return fetch;
    }

    public boolean getOptional() {
        return optional;
    }
}
