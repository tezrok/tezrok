package io.tezrok.spring.relation;

import io.tezrok.api.model.node.FieldNode;
import org.apache.commons.lang3.Validate;

import javax.persistence.EnumType;
import javax.persistence.GenerationType;
import javax.persistence.TemporalType;

public class BasicColumnInfo extends ColumnInfo {
    private final boolean isId;
    private final GenerationType generationType;
    private final TemporalType temporalType;
    private final Column column;
    private final EnumType enumType;

    public BasicColumnInfo(FieldNode field, Column column) {
        this(field, column, false, null, null, null);
    }

    public BasicColumnInfo(FieldNode field, Column column, boolean isId,
                           GenerationType generationType) {
        this(field, column, isId, generationType, null, null);
    }

    public BasicColumnInfo(FieldNode field, Column column, EnumType enumType) {
        this(field, column, false, null, null, enumType);
    }

    private BasicColumnInfo(FieldNode field, Column column, boolean isId,
                            GenerationType generationType, TemporalType temporalType, EnumType enumType) {
        super(field);
        this.column = Validate.notNull(column, "column");
        this.isId = isId;
        this.generationType = generationType;
        this.temporalType = temporalType;
        this.enumType = enumType;
    }

    public boolean isId() {
        return isId;
    }

    public GenerationType getGenerationType() {
        return generationType;
    }

    public TemporalType getTemporalType() {
        return temporalType;
    }

    public Column getColumn() {
        return column;
    }

    public EnumType getEnumType() {
        return enumType;
    }

    public boolean isEnum() {
        return getEnumType() != null;
    }

    public static class Column {
        private final String name;
        private final String dbType;
        private final Long length;
        private final boolean isNullable;
        private final boolean isUnique;
        private final boolean isPrimary;
        private final boolean autoIncrement;

        public Column(String name, String dbType, Long length, boolean isNullable, boolean isUnique,
                      boolean isPrimary, boolean autoIncrement) {
            this.name = Validate.notBlank(name, "name");
            this.dbType = Validate.notBlank(dbType, "dbType");
            this.length = length;
            this.isNullable = isNullable;
            this.isUnique = isUnique;
            this.isPrimary = isPrimary;
            this.autoIncrement = autoIncrement;
        }

        public String getName() {
            return name;
        }

        public Long getLength() {
            return length;
        }

        public boolean isNullable() {
            return isNullable;
        }

        public boolean isUnique() {
            return isUnique;
        }

        public boolean isPrimary() {
            return isPrimary;
        }

        public boolean isAutoIncrement() {
            return autoIncrement;
        }

        public String getDbType() {
            return dbType;
        }

        @Override
        public String toString() {
            return "Column{" +
                    "name='" + name + '\'' +
                    ", dbType='" + dbType + '\'' +
                    ", length=" + length +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "BasicColumnInfo{" +
                "isId=" + isId +
                ", generationType=" + generationType +
                ", temporalType=" + temporalType +
                ", column=" + column +
                ", enumType=" + enumType +
                '}';
    }
}
