package io.tezrok.spring.relation;

import io.tezrok.api.builder.type.EnumNodeType;
import io.tezrok.api.builder.type.Type;
import io.tezrok.api.model.node.EntityNode;
import io.tezrok.api.model.node.FieldNode;
import io.tezrok.api.model.node.ModuleNode;
import io.tezrok.api.model.node.ProjectNode;
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
 * Supports io.tezrok.spring.relation information between entities.
 */
public class EntityRelationResolver {
    private final ProjectNode project;
    private final List<ColumnInfo> columnInfos = new ArrayList<>();

    public EntityRelationResolver(ProjectNode project) {
        this.project = Validate.notNull(project, "project");
    }

    public void init() {
        for(ModuleNode module : project.modules()) {
            for (EntityNode entity : module.entities()) {
                for (FieldNode field : entity.fields()) {
                    columnInfos.add(createInfoFromField(field));
                }
            }
        }
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

    private ColumnInfo createInfoFromField(FieldNode field) {
        final Type type = field.getType();

        if (type.isPrimitive()) {
            return createInfoForPrimitive(field);
        } else if (type.isEntity()) {
            return createInfoForEntity(field);
        } else if (type.isGeneric()) {
            return createInfoForGeneric(field);
        } else if (type.isEnum()) {
            return createInfoForEnum(field);
        }

        throw new IllegalStateException("Unsupported field type: " + type);
    }

    private ColumnInfo createInfoForEnum(FieldNode field) {
        EnumNodeType type = field.getType().as();
        String dbType = type.mapSimpleDbType(field);
        BasicColumnInfo.Column column = new BasicColumnInfo.Column(getDbFieldName(field),
                dbType, EnumNode.NAME_MAX_SIZE, field.isNullable(), false, false, false);

        return new BasicColumnInfo(field, column, EnumType.STRING);
    }

    private ColumnInfo createInfoForPrimitive(FieldNode field) {
        PrimitiveType type = field.getType().as();
        String dbType = type.mapSimpleDbType(field);
        boolean autoIncrement = field.getType().getName().equals("Long") && field.isPrimary();
        BasicColumnInfo.Column column = new BasicColumnInfo.Column(getDbFieldName(field), dbType,
                field.getMax(), field.isNullable(), field.isUnique(), field.isPrimary(), autoIncrement);

        if (field.isPrimary()) {
            return new BasicColumnInfo(field, column, true, GenerationType.IDENTITY);
        }

        return new BasicColumnInfo(field, column);
    }

    private ColumnInfo createInfoForGeneric(FieldNode field) {
        GenericType type = field.getType().as();

        if (!type.getParameterType().isEntity()) {
            throw new IllegalStateException(String.format("Unsupported parameter type '%s'. Supporting only entity type for generic for field '%s'",
                    type.getParameterType(), NameUtil.fieldName(field)));
        }

        // Set<SomeEntityTarget> field;
        NamedNodeType entityNodeType = type.getParameterType().as();
        EntityNode targetEntity = (EntityNode) entityNodeType.getNode();
        Optional<FieldNode> foundTargetField = getTargetEntityField(field, targetEntity);

        if (foundTargetField.isPresent()) {
            FieldNode targetField = foundTargetField.get();

            if (targetField.getType().isEntity()) {
                return new OneToManyColumnInfo(field, targetField);
            } else if (targetField.getType().isGeneric()) {
                JoinTable joinTable = makeJoinTable(field.getEntity(), targetEntity, field);

                return new ManyToManyColumnInfo(field, joinTable, getFetchType(field));
            }

            throw new IllegalStateException("Unsupported target field type: " + targetField.getType());
        }

        // unidirectional io.tezrok.spring.relation
        JoinTable joinTable = makeJoinTable(field.getEntity(), targetEntity, field);

        return new ManyToManyColumnInfo(field, joinTable, getFetchType(field));
    }

    /**
     * Gets io.tezrok.spring.relation type for field with entity type.
     */
    private ColumnInfo createInfoForEntity(FieldNode field) {
        NamedNodeType type = field.getType().as();
        EntityNode targetEntity = (EntityNode) type.getNode();
        Optional<FieldNode> foundTargetField = getTargetEntityField(field, targetEntity);

        if (foundTargetField.isPresent()) {
            FieldNode targetField = foundTargetField.get();

            if (targetField.getType().isEntity()) {
                String fkName = getDbForeignKeyName(field.getEntity().getName(), targetEntity.getName(),
                        getDbTablePrimaryFieldForeignId(targetEntity));
                JoinColumn joinColumn = makeJoinColumn(targetEntity, fkName,
                         field.isNullable());

                return new OneToOneColumnInfo(field, joinColumn, getFetchType(field));
            } else if (targetField.getType().isGeneric()) {
                String fkName = getDbForeignKeyName(field.getEntity().getName(), targetEntity.getName(),
                        getDbTablePrimaryFieldForeignId(targetEntity));
                JoinColumn joinColumn = makeJoinColumn(targetEntity, fkName,
                        field.isNullable());

                return new ManyToOneColumnInfo(field, joinColumn, getFetchType(field), field.isNullable());
            }

            throw new IllegalStateException("Unsupported target field type: " + targetField.getType());
        }

        // unidirectional io.tezrok.spring.relation
        List<RelationType> allowedRelations = Arrays.asList(RelationType.OneToOne, RelationType.ManyToOne);

        if (!field.hasRelation()) {
            throw new IllegalStateException(String.format("Relation for field '%s' must be specified. Allowed types: %s",
                    NameUtil.fieldName(field), allowedRelations));
        }

        validateAllowedRelations(field, allowedRelations);

        switch (field.getRelation()) {
            case OneToOne: {
                String fkName = getDbForeignKeyName(field.getEntity().getName(), targetEntity.getName(),
                        getDbTablePrimaryFieldForeignId(targetEntity));
                JoinColumn joinColumn = makeJoinColumn(targetEntity, fkName, field.isNullable());

                return new OneToOneColumnInfo(field, joinColumn, getFetchType(field));
            }
            case ManyToOne: {
                String fkName = getDbForeignKeyName(field.getEntity().getName(), targetEntity.getName(),
                        getDbTablePrimaryFieldForeignId(targetEntity));
                JoinColumn joinColumn = makeJoinColumn(targetEntity, fkName, field.isNullable());
                Boolean optional = field.isNullable();

                return new ManyToOneColumnInfo(field, joinColumn, getFetchType(field), optional);
            }
            default:
                throw new IllegalStateException(
                        String.format("Invalid io.tezrok.spring.relation '%s' for field '%s'. Expected: %s",
                                field.getRelation(), NameUtil.fieldName(field), allowedRelations));
        }
    }

    private JoinColumn makeJoinColumn(EntityNode targetEntity, String fkName, boolean isNullable) {
        FieldNode primaryField = targetEntity.getPrimaryField();
        String dbType = primaryField.getType().mapSimpleDbType(primaryField);
        String targetTableName = getDbTableName(targetEntity);

        return new JoinColumn(getDbTablePrimaryFieldForeignId(targetEntity),
                getDbTablePrimaryFieldId(targetEntity), dbType, targetTableName, fkName, isNullable);
    }

    private void validateAllowedRelations(FieldNode field, List<RelationType> allowedList) {
        if (field.hasRelation()) {
            if (!allowedList.contains(field.getRelation())) {
                throw new IllegalStateException(
                        String.format("Invalid io.tezrok.spring.relation '%s' for field '%s'. Expected: %s",
                                field.getRelation(), NameUtil.fieldName(field), allowedList.toString()));
            }
        }
    }

    private JoinTable makeJoinTable(EntityNode entity, EntityNode targetEntity, FieldNode field) {
        String fkName1 = getDbForeignKeyName(field.getEntity().getName(), targetEntity.getName(),
                getDbTablePrimaryFieldForeignId(entity));
        JoinColumn joinColumn = makeJoinColumn(entity, fkName1, field.isNullable());
        String fkName2 = getDbForeignKeyName(field.getEntity().getName(), targetEntity.getName(),
                getDbTablePrimaryFieldForeignId(targetEntity));
        JoinColumn inverseJoinColumn = makeJoinColumn(targetEntity, fkName2, field.isNullable());

        return new JoinTable(getDbJoinTableName(entity, targetEntity), joinColumn, inverseJoinColumn);
    }

    private Optional<FieldNode> getTargetEntityField(FieldNode field, EntityNode targetEntity) {
        EntityNode entitySource = field.getEntity();

        List<FieldNode> targetFields = targetEntity.getFields()
                .stream()
                .filter(f -> !field.equals(f) && isTypeOfEntity(f, entitySource)) // entity can refer to itself
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
    private boolean isTypeOfEntity(FieldNode fieldTarget, EntityNode entitySource) {
        if (fieldTarget.getType().isEntity()) {
            NamedNodeType type = fieldTarget.getType().as();

            return type.getNode().equals(entitySource);
        } else if (fieldTarget.getType().isGeneric()) {
            GenericType type = fieldTarget.getType().as();

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
        return normalizeDbTableName(NameUtil.camelCaseToUnderscoreName(entity.getName()) + "_" + NameUtil.camelCaseToUnderscoreName(targetEntity.getName()));
    }

    private String getDbTablePrimaryFieldId(EntityNode entity) {
        FieldNode field = entity.getPrimaryField();

        return NameUtil.camelCaseToUnderscoreName(field.getName());
    }

    /**
     * Gets name of foreign column for entity.
     */
    private String getDbTablePrimaryFieldForeignId(EntityNode entity) {
        return NameUtil.camelCaseToUnderscoreName(entity.getName()) + "_" + getDbTablePrimaryFieldId(entity);
    }

    public String getDbForeignKeyName(String tableName, String targetTableName, String columnName) {
        return "fk_" + NameUtil.camelCaseToUnderscoreName(tableName) + "_"
                + NameUtil.camelCaseToUnderscoreName(targetTableName) + "_" + columnName;
    }

    public String getDbTableName(EntityNode targetEntity) {
        return normalizeDbTableName(NameUtil.camelCaseToUnderscoreName(targetEntity.getName()));
    }

    private String normalizeDbTableName(String rawName) {
        return "T_" + rawName.toUpperCase();
    }

    private String getDbFieldName(FieldNode field) {
        return NameUtil.camelCaseToUnderscoreName(field.getName());
    }
}
