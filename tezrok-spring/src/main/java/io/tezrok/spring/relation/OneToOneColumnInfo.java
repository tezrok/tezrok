package io.tezrok.spring.relation;

import io.tezrok.api.model.node.FieldNode;
import org.apache.commons.lang3.Validate;

import javax.persistence.FetchType;

public class OneToOneColumnInfo extends ColumnInfo {
    private final JoinColumn joinColumn;
    private final FetchType fetchType;

    public OneToOneColumnInfo(FieldNode field, JoinColumn joinColumn, FetchType fetchType) {
        super(field);
        this.fetchType = Validate.notNull(fetchType, "fetchType");
        this.joinColumn = Validate.notNull(joinColumn, "joinColumn");
    }

    public JoinColumn getJoinColumn() {
        return joinColumn;
    }

    public FetchType getFetchType() {
        return fetchType;
    }
}
