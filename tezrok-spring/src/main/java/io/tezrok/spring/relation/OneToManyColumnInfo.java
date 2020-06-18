package io.tezrok.spring.relation;

import io.arusland.tezrok.parser.EntityFieldNode;
import org.apache.commons.lang3.Validate;

/**
 * Created by ruslan on 02.04.2016.
 */
public class OneToManyColumnInfo extends ColumnInfo {
    private final EntityFieldNode mappedBy;

    public OneToManyColumnInfo(EntityFieldNode field, EntityFieldNode mappedBy) {
        super(field);
        this.mappedBy = Validate.notNull(mappedBy, "mappedBy");
    }

    public EntityFieldNode getMappedBy() {
        return mappedBy;
    }
}
