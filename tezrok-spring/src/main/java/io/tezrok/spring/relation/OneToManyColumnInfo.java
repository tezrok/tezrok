package io.tezrok.spring.relation;

import io.tezrok.api.model.node.FieldNode;
import org.apache.commons.lang3.Validate;

/**
 * Created by ruslan on 02.04.2016.
 */
public class OneToManyColumnInfo extends ColumnInfo {
    private final FieldNode mappedBy;

    public OneToManyColumnInfo(FieldNode field, FieldNode mappedBy) {
        super(field);
        this.mappedBy = Validate.notNull(mappedBy, "mappedBy");
    }

    public FieldNode getMappedBy() {
        return mappedBy;
    }
}
