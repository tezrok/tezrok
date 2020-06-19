package io.tezrok.spring.relation;

import io.tezrok.api.GlobalContext;
import io.tezrok.api.builder.type.*;
import io.tezrok.api.model.node.*;
import io.tezrok.spring.util.NameUtil;
import org.apache.commons.lang3.Validate;

import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.GenerationType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Supports relation information between entities
 */
public class EntityRelationResolver {
    private final ProjectNode project;
    private final List<ColumnInfo> columnInfos = new ArrayList<>();

    public EntityRelationResolver(ProjectNode project) {
        this.project = Validate.notNull(project, "project");
    }

    public EntityRelationResolver init(GlobalContext context) {
        for (ModuleNode module : project.modules()) {
            for (EntityNode entity : module.entities()) {
                for (FieldNode field : entity.fields()) {
                    columnInfos.add(createInfoFromField(field, context));
                }
            }
        }
        return this;
    }

    public ColumnInfo getInfo(FieldNode field) {
        Optional<ColumnInfo> info = columnInfos.stream()
                .filter(p -> p.getField().equals(field))
                .findFirst();

        if (info.isPresent()) {
            return info.get();
        }

        throw new IllegalStateException("Field not found: " + field);
    }

    private ColumnInfo createInfoFromField(FieldNode field, GlobalContext context) {
        final Type type = context.resolveType(field);

        if (type.isPrimitive()) {
            return createInfoForPrimitive(field, type);
        } else if (type.isEntity()) {
            return createInfoForEntity(field, type, context);
        } else if (type.isGeneric()) {
            return createInfoForGeneric(field, type, context);
        } else if (type.isEnum()) {
            return createInfoForEnum(field, type);
        }

        throw new IllegalStateException("Unsupported field type: " + type);
    }

    private ColumnInfo createInfoForEnum(FieldNode field, Type type) {
        String dbType = DbTypeMapper.mapSimpleDbType(field, type);
        BasicColumnInfo.Column column = new BasicColumnInfo.Column(getDbFieldName(field),
                dbType, EnumNode.NAME_MAX_SIZE, field.isNullable(), false, false, false);

        return new BasicColumnInfo(field, column, EnumType.STRING);
    }

    private ColumnInfo createInfoForPrimitive(FieldNode field, Type type) {
        String dbType = DbTypeMapper.mapSimpleDbType(field, type);
        boolean autoIncrement = type.getName().equals("Long") && field.getPrimary();
        BasicColumnInfo.Column column = new BasicColumnInfo.Column(getDbFieldName(field), dbType,
                field.getMax(), field.isNullable(), field.getUnique(), field.getPrimary(), autoIncrement);

        if (field.getPrimary()) {
            return new BasicColumnInfo(field, column, true, GenerationType.IDENTITY);
        }

        return new BasicColumnInfo(field, column);
    }

    private ColumnInfo createInfoForGeneric(FieldNode field, Type typeIn, GlobalContext context) {
        GenericType type = typeIn.as();

        if (!type.getParameterType().isEntity()) {
            throw new IllegalStateException(String.format("Unsupported parameter type '%s'. Supporting only entity type for generic for field '%s'",
                    type.getParameterType(), NameUtil.INSTANCE.fieldName(field)));
        }

        NamedNodeType entityNodeType = type.getParameterType().as();
        EntityNode targetEntity = (EntityNode) entityNodeType.getNode();
        Optional<FieldNode> foundTargetField = getTargetEntityField(field, targetEntity, context);

        if (foundTargetField.isPresent()) {
            FieldNode targetField = foundTargetField.get();
            Type targetType = context.resolveType(targetField);

            if (targetType.isEntity()) {
                return new OneToManyColumnInfo(field, targetField);
            } else if (targetType.isGeneric()) {
                JoinTable joinTable = makeJoinTable(field.getParent(), targetEntity, field, context);

                return new ManyToManyColumnInfo(field, joinTable, getFetchType(field));
            }

            throw new IllegalStateException("Unsupported target field type: " + targetField.getType());
        }

        // unidirectional io.tezrok.spring.relation
        JoinTable joinTable = makeJoinTable(field.getParent(), targetEntity, field, context);

        return new ManyToManyColumnInfo(field, joinTable, getFetchType(field));
    }

    /**
     * Gets io.tezrok.spring.relation type for field with entity type.
     */
    private ColumnInfo createInfoForEntity(FieldNode field, Type typeIn, GlobalContext context) {
        NamedNodeType type = typeIn.as();
        EntityNode targetEntity = (EntityNode) type.getNode();
        Optional<FieldNode> foundTargetField = getTargetEntityField(field, targetEntity, context);

        if (foundTargetField.isPresent()) {
            FieldNode targetField = foundTargetField.get();
            Type targetType = context.resolveType(targetField);

            if (targetType.isEntity()) {
                String fkName = getDbForeignKeyName(field.getParent().getName(), targetEntity.getName(),
                        getDbTablePrimaryFieldForeignId(targetEntity));
                JoinColumn joinColumn = makeJoinColumn(targetEntity, fkName,
                        field.isNullable(), context);

                return new OneToOneColumnInfo(field, joinColumn, getFetchType(field));
            } else if (targetType.isGeneric()) {
                String fkName = getDbForeignKeyName(field.getParent().getName(), targetEntity.getName(),
                        getDbTablePrimaryFieldForeignId(targetEntity));
                JoinColumn joinColumn = makeJoinColumn(targetEntity, fkName,
                        field.isNullable(), context);

                return new ManyToOneColumnInfo(field, joinColumn, getFetchType(field), field.isNullable());
            }

            throw new IllegalStateException("Unsupported target field type: " + targetField.getType());
        }

        // unidirectional io.tezrok.spring.relation
        List<RelationType> allowedRelations = Arrays.asList(RelationType.OneToOne, RelationType.ManyToOne);

        if (!field.hasRelation()) {
            throw new IllegalStateException(String.format("Relation for field '%s' must be specified. Allowed types: %s",
                    NameUtil.INSTANCE.fieldName(field), allowedRelations));
        }

        validateAllowedRelations(field, allowedRelations);

        switch (field.getRelation()) {
            case OneToOne: {
                String fkName = getDbForeignKeyName(field.getParent().getName(), targetEntity.getName(),
                        getDbTablePrimaryFieldForeignId(targetEntity));
                JoinColumn joinColumn = makeJoinColumn(targetEntity, fkName, field.isNullable(), context);

                return new OneToOneColumnInfo(field, joinColumn, getFetchType(field));
            }
            case ManyToOne: {
                String fkName = getDbForeignKeyName(field.getParent().getName(), targetEntity.getName(),
                        getDbTablePrimaryFieldForeignId(targetEntity));
                JoinColumn joinColumn = makeJoinColumn(targetEntity, fkName, field.isNullable(), context);
                Boolean optional = field.isNullable();

                return new ManyToOneColumnInfo(field, joinColumn, getFetchType(field), optional);
            }
            default:
                throw new IllegalStateException(
                        String.format("Invalid io.tezrok.spring.relation '%s' for field '%s'. Expected: %s",
                                field.getRelation(), NameUtil.INSTANCE.fieldName(field), allowedRelations));
        }
    }

    private JoinColumn makeJoinColumn(EntityNode targetEntity, String fkName, boolean isNullable, GlobalContext context) {
        FieldNode primaryField = targetEntity.getPrimaryField();
        Type type = context.resolveType(primaryField);
        String dbType = DbTypeMapper.mapSimpleDbType(primaryField, type);
        String targetTableName = getDbTableName(targetEntity);

        return new JoinColumn(getDbTablePrimaryFieldForeignId(targetEntity),
                getDbTablePrimaryFieldId(targetEntity), dbType, targetTableName, fkName, isNullable);
    }

    private void validateAllowedRelations(FieldNode field, List<RelationType> allowedList) {
        if (field.hasRelation()) {
            if (!allowedList.contains(field.getRelation())) {
                throw new IllegalStateException(
                        String.format("Invalid io.tezrok.spring.relation '%s' for field '%s'. Expected: %s",
                                field.getRelation(), NameUtil.INSTANCE.fieldName(field), allowedList.toString()));
            }
        }
    }

    private JoinTable makeJoinTable(EntityNode entity, EntityNode targetEntity, FieldNode field, GlobalContext context) {
        String fkName1 = getDbForeignKeyName(field.getParent().getName(), targetEntity.getName(),
                getDbTablePrimaryFieldForeignId(entity));
        JoinColumn joinColumn = makeJoinColumn(entity, fkName1, field.isNullable(), context);
        String fkName2 = getDbForeignKeyName(field.getParent().getName(), targetEntity.getName(),
                getDbTablePrimaryFieldForeignId(targetEntity));
        JoinColumn inverseJoinColumn = makeJoinColumn(targetEntity, fkName2, field.isNullable(), context);

        return new JoinTable(getDbJoinTableName(entity, targetEntity), joinColumn, inverseJoinColumn);
    }

    private Optional<FieldNode> getTargetEntityField(FieldNode field, EntityNode targetEntity, GlobalContext context) {
        EntityNode entitySource = field.getParent();

        List<FieldNode> targetFields = targetEntity.fields()
                .stream()
                .filter(f -> !field.equals(f) && isTypeOfEntity(f, entitySource, context)) // entity can refer to itself
                .collect(Collectors.toList());

        if (targetFields.isEmpty()) {
            return Optional.empty();
        }

        if (targetFields.size() > 1) {
            String fieldNames = Arrays.toString(targetFields.stream().map(f -> f.getName()).toArray());
            throw new IllegalStateException(String.format("Found several fields {%s} in target entity '%s'. Expected only one.",
                    fieldNames, targetEntity.getName()));
        }

        return Optional.of(targetFields.get(0));
    }

    /**
     * Checks if field of target entity contains fields with source entity.
     */
    private boolean isTypeOfEntity(FieldNode fieldTarget, EntityNode entitySource, GlobalContext context) {
        Type targetType = context.resolveType(fieldTarget);

        if (targetType.isEntity()) {
            NamedNodeType type = targetType.as();

            return type.getNode().equals(entitySource);
        } else if (targetType.isGeneric()) {
            GenericType type = targetType.as();

            if (type.getParameterType().isEntity()) {
                NamedNodeType entityType = type.getParameterType().as();

                return entityType.equals(entitySource);
            }
        }

        return false;
    }

    private static FetchType getFetchType(FieldNode field) {
        return field.isLazy() ? FetchType.LAZY : FetchType.EAGER;
    }

    private String getDbJoinTableName(EntityNode entity, EntityNode targetEntity) {
        return normalizeDbTableName(NameUtil.INSTANCE.camelCaseToUnderscoreName(entity.getName())
                + "_" + NameUtil.INSTANCE.camelCaseToUnderscoreName(targetEntity.getName()));
    }

    private String getDbTablePrimaryFieldId(EntityNode entity) {
        FieldNode field = entity.getPrimaryField();

        return NameUtil.INSTANCE.camelCaseToUnderscoreName(field.getName());
    }

    /**
     * Gets name of foreign column for entity.
     */
    private String getDbTablePrimaryFieldForeignId(EntityNode entity) {
        return NameUtil.INSTANCE.camelCaseToUnderscoreName(entity.getName()) + "_" + getDbTablePrimaryFieldId(entity);
    }

    public String getDbForeignKeyName(String tableName, String targetTableName, String columnName) {
        return "fk_" + NameUtil.INSTANCE.camelCaseToUnderscoreName(tableName) + "_"
                + NameUtil.INSTANCE.camelCaseToUnderscoreName(targetTableName) + "_" + columnName;
    }

    public String getDbTableName(EntityNode targetEntity) {
        return normalizeDbTableName(NameUtil.INSTANCE.camelCaseToUnderscoreName(targetEntity.getName()));
    }

    private String normalizeDbTableName(String rawName) {
        return "T_" + rawName.toUpperCase();
    }

    private String getDbFieldName(FieldNode field) {
        return NameUtil.INSTANCE.camelCaseToUnderscoreName(field.getName());
    }
}
