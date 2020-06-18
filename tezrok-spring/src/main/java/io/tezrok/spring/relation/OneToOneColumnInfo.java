package io.tezrok.spring.relation;

import io.arusland.tezrok.parser.EntityFieldNode;
import org.apache.commons.lang3.Validate;

import javax.persistence.FetchType;

/**
 * Created by ruslan on 02.04.2016.
 */
public class OneToOneColumnInfo extends ColumnInfo {
    private final JoinColumn joinColumn;
    private final FetchType fetchType;

    public OneToOneColumnInfo(EntityFieldNode field, JoinColumn joinColumn, FetchType fetchType) {
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
