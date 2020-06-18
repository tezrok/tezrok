package io.tezrok.spring.relation;

import org.apache.commons.lang3.Validate;

/**
 * Created by ruslan on 02.04.2016.
 */
public class JoinColumn {
    private final String name;
    private final String referencedColumnName;
    private final String dbType;
    private final String targetTableName;
    private final String foreignKeyName;
    private final boolean nullable;

    public JoinColumn(String name, String referencedColumnName, String dbType, String targetTableName, String foreignKeyName, boolean nullable) {
        this.nullable = nullable;
        this.name = Validate.notBlank(name, "name");
        this.referencedColumnName = Validate.notNull(referencedColumnName, "referencedColumnName");
        this.dbType = Validate.notBlank(dbType, "dbType");
        this.targetTableName = Validate.notBlank(targetTableName, "targetTableName");
        this.foreignKeyName = Validate.notBlank(foreignKeyName, "foreignKeyName");
    }

    public String getName() {
        return name;
    }

    public String getReferencedColumnName() {
        return referencedColumnName;
    }

    public String getDbType() {
        return dbType;
    }

    public String getTargetTableName() {
        return targetTableName;
    }

    public String getForeignKeyName() {
        return foreignKeyName;
    }

    public boolean isNullable() {
        return nullable;
    }
}
