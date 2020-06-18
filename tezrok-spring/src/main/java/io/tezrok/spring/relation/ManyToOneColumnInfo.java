package io.tezrok.spring.relation;

import io.arusland.tezrok.parser.EntityFieldNode;
import org.apache.commons.lang3.Validate;

import javax.persistence.FetchType;

/**
 * Created by ruslan on 02.04.2016.
 */
public class ManyToOneColumnInfo extends ColumnInfo {
    private final JoinColumn joinColumn;
    private final FetchType fetch;
    private final boolean optional;

    public ManyToOneColumnInfo(EntityFieldNode field, JoinColumn joinColumn, FetchType fetch, boolean optional) {
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
