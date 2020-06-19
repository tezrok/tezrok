package io.tezrok.spring.relation;

import io.tezrok.api.model.node.FieldNode;
import org.apache.commons.lang3.Validate;

import javax.persistence.FetchType;

public class ManyToManyColumnInfo extends ColumnInfo {
    private final JoinTable joinTable;
    private final FetchType fetchType;

    public ManyToManyColumnInfo(FieldNode field, JoinTable joinTable, FetchType fetchType) {
        super(field);
        this.fetchType = Validate.notNull(fetchType, "fetchType");
        this.joinTable = Validate.notNull(joinTable, "joinTable");
    }

    public JoinTable getJoinTable() {
        return joinTable;
    }

    public FetchType getFetchType() {
        return fetchType;
    }
}
