package io.tezrok.spring.relation;

import io.arusland.tezrok.parser.EntityFieldNode;
import org.apache.commons.lang3.Validate;

import javax.persistence.FetchType;

/**
 * Created by ruslan on 02.04.2016.
 */
public class ManyToManyColumnInfo extends ColumnInfo {
    private final JoinTable joinTable;
    private final FetchType fetchType;

    public ManyToManyColumnInfo(EntityFieldNode field, JoinTable joinTable, FetchType fetchType) {
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
