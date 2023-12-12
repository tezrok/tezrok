package io.tezrok.jooq

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.stmt.Statement
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.*
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.java.JavaFieldNode
import io.tezrok.api.java.JavaMethodNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.*
import org.apache.commons.collections4.CollectionUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Collectors

/**
 * Creates EntityGraphStore class
 */
internal class EntityGraphStoreFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val projectElem = context.getProject()
            val repositoryDir = applicationPackageRoot.getOrAddJavaDirectory("repository")
            addEntityUpdateType(repositoryDir, projectElem.packagePath, context)

            val schemaModule = context.getProject().modules.find { it.name == module.getName() }
                ?: throw IllegalStateException("Module ${module.getName()} not found")
            schemaModule.schema?.entities?.let { entities ->
                addEntityGraphStore(repositoryDir, entities)
            }
        } else {
            log.warn("Application package root is not set")
        }

        return true
    }

    private fun addEntityGraphStore(
        repositoryDir: JavaDirectoryNode,
        entities: List<EntityElem>
    ) {
        val clazz = repositoryDir.addClass("EntityGraphStore")
            .addAnnotation(Service::class.java)
            .setModifiers(Modifier.Keyword.PUBLIC)
            .setJavadocComment(
                """Stores whole entity by id with all relations.
<p>                
Entity save/update strategy depends on {@link EntityUpdateType}.

@see EntityUpdateType"""
            )
            .addImport(List::class.java)
            .addImport(ArrayList::class.java)
            .addImport(Set::class.java)
            .addImport(HashSet::class.java)
            .addImport(Collectors::class.java)
            .addImport(Collections::class.java)
            .addImport(CollectionUtils::class.java)
            .addImport(org.apache.commons.lang3.tuple.Pair::class.java)

        // add fields which are initialized in constructor
        val fields = mutableListOf<JavaFieldNode>()
        entities.forEach { entity ->
            addFields(clazz, entity, fields)
        }
        fields.forEach(clazz::initInConstructor)

        // add public load methods
        entities.filter { it.isNotSynthetic() }.forEach { entity ->
            clazz.addImportBySimpleName(entity.getDtoName())
            clazz.addImportBySimpleName(entity.getFullDtoName())
            addPublicSaveFullEntityMethods(clazz, entity)
        }

        // add InnerContext class
        val contextClass = addInnerContextClass(clazz, entities)

        // add internal load methods
        val entitiesMap = EntitiesMap.from(entities)
        val methods = mutableMapOf<EntityElem, JavaMethodNode>()
        entities.filter { it.isNotSynthetic() }.forEach { entity ->
            methods[entity] = addSaveFullEntityMethods(contextClass, entity, entitiesMap)
        }

        // implement saveFullEntity methods
        methods.forEach { (entity, method) ->
            implementSaveFullEntityMethod(method, entity, entitiesMap)
        }

        entities.filter { it.isNotSynthetic() }
            .forEach { entity ->
                addLoadEntityIdFieldsOrReturnMethod(contextClass, entity)
            }

        addCloseMethod(contextClass, entities)
    }

    private fun addLoadEntityIdFieldsOrReturnMethod(clazz: JavaClassNode, entity: EntityElem) {
        val fullDtoName = entity.getFullDtoName()
        val entityIdType = entity.getPrimaryFieldType()
        val dtoName = entity.getDtoName()
        val statements = NodeList<Statement>()
        val method = clazz.addMethod(entity.getLoadEntityIdFieldsOrReturnMethod())
            .addParameter(fullDtoName, "newFullDto")
            .setReturnType("Pair<$entityIdType, $dtoName>")
            .setJavadocComment("Return left value (ID) if we need to return, otherwise id fields of {@link $dtoName}.")

        val entityField = entity.name.lowerFirst()
        val entityIdsSaved = """${entityField}IdsSaved"""
        val entityRepository = entity.getRepositoryName().lowerFirst()
        val hasIdFields = entity.getIdFields().size > 1
        val primaryField = entity.getPrimaryField()
        val primaryIdGetter = primaryField.getGetterName()
        if (hasIdFields) {
            statements.addAll(
                """if (newFullDto.getId() != null) {
                if ($entityIdsSaved.contains(newFullDto.getId())) {
                    return Pair.of(newFullDto.getId(), null);
                }
                $entityIdsSaved.add(newFullDto.getId());

                return Pair.of(null, $entityRepository.getIdFieldsBy${primaryField.name.upperFirst()}(newFullDto.$primaryIdGetter(), $dtoName.class));
            }""".parseAsStatements()
            )
        } else {
            statements.addAll(
                """if (newFullDto.getId() != null) {
                if ($entityIdsSaved.contains(newFullDto.getId())) {
                    return Pair.of(newFullDto.getId(), null);
                }
                $entityIdsSaved.add(newFullDto.getId());

                return Pair.of(null, null);
            }""".parseAsStatements()
            )
        }

        entity.fields.find { it.uniqueGroup?.isNotBlank() == true }?.let { error("Unique group is not supported yet") }

        val uniqueFields = entity.getUniqueStringFields()

        if (uniqueFields.isNotEmpty()) {
            var second = false
            var code = "// check unique fields\n"
            uniqueFields.forEach { field ->
                val idFieldsByUniqField = entity.getGetIdFieldsByUniqueField(field)
                val primaryFieldByUniqField = entity.getGetPrimaryIdFieldByUniqueField(field)
                val fieldGetter = field.getGetterName()
                val elsePrefix = if (second) " else " else ""
                code += """${elsePrefix}if (newFullDto.$fieldGetter() != null) {
                if (updateType == EntityUpdateType.UPDATE_RELATION_BY_NAME) {
                    // if we don't have id, have unique field and need update only relation, return only id
                    final Long idByName = $entityRepository.$primaryFieldByUniqField(newFullDto.$fieldGetter());
                    if (idByName != null) {
                        return Pair.of(idByName, null);
                    }
                }
            }"""
                // case when we have uniqfield and id fields
                if (hasIdFields) {
                    code += """final $dtoName oldIdFields;
                if (updateType == EntityUpdateType.UPDATE_BY_NAME) {
                    // if we have unique field, we can load id fields by it
                    oldIdFields = $entityRepository.$idFieldsByUniqField(newFullDto.$fieldGetter(), $dtoName.class);
                } else {
                    oldIdFields = null;
                }

                if (oldIdFields != null) {
                    if ($entityIdsSaved.contains(oldIdFields.$primaryIdGetter())) {
                        return Pair.of(oldIdFields.$primaryIdGetter(), null);
                    }
                    $entityIdsSaved.add(oldIdFields.$primaryIdGetter());
                    return Pair.of(null, oldIdFields);
                }"""
                }
                second = true
            }

            statements.addAll(code.parseAsStatements())
        }

        statements.add("return Pair.of(null, null);".parseAsStatement())
        method.setBody(statements)
    }

    private fun implementSaveFullEntityMethod(method: JavaMethodNode, entity: EntityElem, entitiesMap: EntitiesMap) {
        val statements = NodeList<Statement>()

        val fullDtoName = entity.getFullDtoName()
        val fullDtoParam = fullDtoName.lowerFirst()
        val entityName = entity.name
        val dtoName = entity.getDtoName()
        val dtoField = dtoName.lowerFirst()
        val primaryField = entity.getPrimaryField()
        val idType = primaryField.asJavaType()
        // Pair<Long, OrderDto> returnOrIds = loadOrderIdFieldsOrReturn(orderFullDto);
        val loadMethod = entity.getLoadEntityIdFieldsOrReturnMethod()
        statements.add(
            "final Pair<$idType, $dtoName> returnOrIds = $loadMethod($fullDtoParam);".parseAsStatement()
                .withLineComment(" old $entityName ids fields")
        )

        // if (returnOrIds.getLeft() != null) { return returnOrIds.getLeft(); }
        statements.add("if (returnOrIds.getLeft() != null) { return returnOrIds.getLeft(); }".parseAsStatement())
        if (entity.fields.any { it.relation == EntityRelation.OneToOne }) {
            // final OrderDto oldIdFields = returnOrIds.getRight();
            statements.add("final $dtoName oldIdFields = returnOrIds.getRight();".parseAsStatement())
        }

        // final OrderDto orderDtoPre = orderMapper.toOrderDto(orderFullDto);
        val mapperField = entity.getMapperName().lowerFirst()
        val dtoPreField = "${dtoField}Pre"
        statements.add("final $dtoName $dtoPreField = $mapperField.to${dtoName}($fullDtoParam);".parseAsStatement())

        val oneToManySyntheticField = entitiesMap.findOneToManySyntheticField(entity)
        if (oneToManySyntheticField != null) {
            // itemDtoPre.setItemsOrderId(itemsOrderId);
            statements.add(
                "$dtoPreField.${oneToManySyntheticField.getSetterName()}(${oneToManySyntheticField.name});"
                    .parseAsStatement().withLineComment(" set synthetic field to support OneToMany relation")
            )
        }

        // generates save OneToOne and ManyToOne fields
        entity.fields.filter { field -> field.relation == EntityRelation.ManyToOne || field.relation == EntityRelation.OneToOne }
            .forEach { field ->
                val syntheticField = entitiesMap.getSyntheticField(entity, field)
                val fieldGetter = syntheticField.getGetterName()
                val refEntity = entitiesMap.getRefEntity(field)
                val fieldSetter = syntheticField.getSetterName()

                if (field.relation == EntityRelation.OneToOne) {
                    // orderDtoPre.setSelectedItemId(saveOneToOneOrder(orderFullDto.getNextOrder(), oldIdFields != null ? oldIdFields.getNextOrderId() : null));
                    val methodName = getOrAddOneToOneMethod(refEntity, method.getOwner()).getName()
                    statements.add(
                        "$dtoPreField.$fieldSetter($methodName($fullDtoParam.${field.getGetterName()}(), oldIdFields != null ? oldIdFields.$fieldGetter() : null));"
                            .parseAsStatement().withLineComment(" save property \"${field.name}\" - OneToOne relation")
                    )
                } else if (field.relation == EntityRelation.ManyToOne) {
                    // orderDtoPre.setSelectedItemId(saveManyToOneItem(orderFullDto.getSelectedItem()));
                    val methodName = getOrAddManyToOneMethod(refEntity, method.getOwner()).getName()
                    statements.add(
                        "$dtoPreField.$fieldSetter($methodName($fullDtoParam.${field.getGetterName()}()));"
                            .parseAsStatement().withLineComment(" save property \"${field.name}\" - ManyToOne relation")
                    )
                }
            }

        // final OrderDto orderDto = orderRepository.save(orderDtoPre);
        val entityRepositoryName = entity.getRepositoryName().lowerFirst()
        statements.add(
            "final $dtoName $dtoField = $entityRepositoryName.save($dtoPreField);"
                .parseAsStatement().withLineComment(" save $entityName")
        )

        // orderIdsSaved.add(orderDto.getId());
        val entityField = entity.name.lowerFirst()
        val primaryFieldCall = "$dtoField.${primaryField.getGetterName()}()"
        statements.add(
            "${entityField}IdsSaved.add($primaryFieldCall);"
                .parseAsStatement()
                .withLineComment(" preserve $entityName from deletion and to avoid infinite recursion")
        )

        entity.fields.filter { field -> field.relation == EntityRelation.OneToMany || field.relation == EntityRelation.ManyToMany }
            .forEach { field ->
                // saveOrderItems(orderFullDto, orderDto.getId());
                val methodName = addToManyMethod(field, entity, entitiesMap, method.getOwner()).getName()
                statements.add(
                    "$methodName($fullDtoParam, $primaryFieldCall);"
                        .parseAsStatement()
                        .withLineComment(" save property \"${field.name}\" - ${field.relation} relation")
                )
            }

        // return orderDto.getId();
        statements.add("return $primaryFieldCall;".parseAsStatement())

        method.setBody(statements)
    }

    private fun getOrAddManyToOneMethod(entity: EntityElem, clazz: JavaClassNode): JavaMethodNode {
        val methodName = "saveManyToOne${entity.name}"
        clazz.getMethod(methodName)?.let { return it }

        val entityName = entity.name
        val paramName = "new$entityName"
        val paramType = entity.getFullDtoName()
        return clazz.addMethod(methodName)
            .addAnnotation(Nullable::class.java)
            .addParameter(paramType, paramName)
            .setReturnType(entity.getPrimaryFieldType())
            .setBody("return $paramName != null ? saveFull$entityName($paramName) : null;".parseAsStatement())
            .setJavadocComment(
                """Save property of type {@link $paramType} and ManyToOne relation.

@param $paramName $entityName to save
@return id of saved $entityName"""
            )
    }

    private fun getOrAddOneToOneMethod(entity: EntityElem, clazz: JavaClassNode): JavaMethodNode {
        val methodName = "saveOneToOne${entity.name}"
        clazz.getMethod(methodName)?.let { return it }

        val entityName = entity.name
        val entityField = entityName.lowerFirst()
        val paramName = "new$entityName"
        val paramType = entity.getFullDtoName()
        val primaryIdType = entity.getPrimaryFieldType()
        return clazz.addMethod(methodName)
            .addAnnotation(Nullable::class.java)
            .addParameter(paramType, paramName)
            .addParameter(primaryIdType, "oldId")
            .setReturnType(primaryIdType)
            .setBody(
                """final Long nextId = $paramName != null ? saveFull$entityName($paramName) : null;
            if (oldId != null && !oldId.equals(nextId)) {
                // delete old $entityName as it's not used anymore and it's OneToOne relation
                ${entityField}IdsToDelete.add(oldId);
            }
            return nextId;""".parseAsBlock()
            )
            .setJavadocComment(
                """Save property of type {@link $paramType} and OneToOne relation.

 @param $paramName new $entityName to save
 @param oldId    old $entityName id
 @return id of saved $entityName"""
            )
    }

    private fun addToManyMethod(
        field: FieldElem,
        entity: EntityElem,
        entities: EntitiesMap,
        clazz: JavaClassNode
    ): JavaMethodNode {
        return when (field.relation) {
            EntityRelation.OneToMany -> addOneToManyMethod(field, entity, entities, clazz)

            EntityRelation.ManyToMany -> addManyToManyMethod(field, entity, entities, clazz)
            else -> error("Unsupported relation: ${field.relation}")
        }
    }

    private fun addOneToManyMethod(
        field: FieldElem,
        entity: EntityElem,
        entities: EntitiesMap,
        clazz: JavaClassNode
    ): JavaMethodNode {
        check(field.relation == EntityRelation.OneToMany) { "Expected OneToMany, but found: ${field.relation}" }

        val methodName = "save${entity.name}${field.name.upperFirst()}"
        val entityName = entity.name
        val paramType = entity.getFullDtoName()
        val paramName = paramType.lowerFirst()
        val primaryFieldType = entity.getPrimaryFieldType()
        val primaryFieldName = "${entityName}Id".lowerFirst()
        val primaryGetter = entity.getPrimaryField().getGetterName()
        val fieldGetter = field.getGetterName()
        val refEntity = entities.getRefEntity(field)
        val refEntityPrimaryFieldType = refEntity.getPrimaryFieldType()
        val refEntityName = refEntity.name
        val refEntityField = refEntityName.lowerFirst()
        val oldEntityIds = "old${refEntityName}Ids"
        val method = clazz.addMethod(methodName)
            .addParameter(paramType, paramName)
            .addParameter(primaryFieldType, primaryFieldName)
        val refRepoName = refEntity.getRepositoryName().lowerFirst()
        val refRepoMethodName = entities.getMethodByField(
            entity,
            field,
            OneToManyMethod.FindRefPrimaryFieldByRefSyntheticField
        ).keys.first()
        val newEntityId = "new${refEntityName}Id"
        method.setBody(
            """// load old ids
            final Set<$refEntityPrimaryFieldType> $oldEntityIds = new HashSet<>($paramName.$primaryGetter() != null ?
                    $refRepoName.$refRepoMethodName($paramName.$primaryGetter()) : Collections.emptyList());
            // save new ${refEntityField}s
            CollectionUtils.emptyIfNull($paramName.$fieldGetter()).forEach(fullDto -> {
                final $refEntityPrimaryFieldType $newEntityId = saveFull$refEntityName(fullDto, $primaryFieldName);
                $oldEntityIds.remove($newEntityId);
            });
            // delete old ${refEntityField}s as OneToMany relation
            ${refEntityField}IdsToDelete.addAll($oldEntityIds);""".parseAsBlock()
        ).setJavadocComment(
            """Save property {@link $paramType#$fieldGetter()} of OneToMany relation.

@param $paramName full $entityName dto
@param $primaryFieldName id of $entityName"""
        )
        return method
    }

    private fun addManyToManyMethod(
        field: FieldElem,
        entity: EntityElem,
        entities: EntitiesMap,
        clazz: JavaClassNode
    ): JavaMethodNode {
        check(field.relation == EntityRelation.ManyToMany) { "Expected ManyToMany, but found: ${field.relation}" }

        val methodName = "save${entity.name}${field.name.upperFirst()}"
        val entityName = entity.name
        val paramType = entity.getFullDtoName()
        val paramName = paramType.lowerFirst()
        val primaryFieldType = entity.getPrimaryFieldType()
        val primaryFieldName = "${entityName}Id".lowerFirst()
        val primaryGetter = entity.getPrimaryField().getGetterName()
        val fieldGetter = field.getGetterName()
        val refEntity = entities.getRefEntity(field)
        val refEntityPrimaryFieldType = refEntity.getPrimaryFieldType()
        val refEntityName = refEntity.name
        val refEntityFullDto = refEntity.getFullDtoName()
        val refEntityField = refEntityName.lowerFirst()
        val oldEntityIds = "old${refEntityName}Ids"
        val method = clazz.addMethod(methodName)
            .addParameter(paramType, paramName)
            .addParameter(primaryFieldType, primaryFieldName)
        val newEntityId = "new${refEntityName}Id"
        val relationEntity = entities.getRelationEntity(entity, field)
        val relationEntityDto = relationEntity.getDtoName()
        val relationEntityRepo = relationEntity.getRepositoryName().lowerFirst()
        val sourceField = relationEntity.fields[0]
        val sourceFieldSetter = sourceField.getSetterName()
        val sourceFieldName = sourceField.name.upperFirst()
        val targetField = relationEntity.fields[1]
        val targetFieldGetter = targetField.getGetterName()
        val targetFieldSetter = targetField.getSetterName()
        val entityList = "${refEntityField}s"
        method.setBody(
            """// load old ids
            final Set<$refEntityPrimaryFieldType> $oldEntityIds = $paramName.$primaryGetter() != null ?
                    $relationEntityRepo.findBy$sourceFieldName($paramName.$primaryGetter()).stream()
                            .map($relationEntityDto::$targetFieldGetter)
                            .collect(Collectors.toSet()) : Collections.emptySet();
            // save new $entityList
            final List<$refEntityFullDto> $entityList = $paramName.$fieldGetter();
            if (CollectionUtils.isNotEmpty($entityList)) {
                final List<$relationEntityDto> newRelations = new ArrayList<>($entityList.size());
                $entityList.forEach($refEntityField -> {
                    final $refEntityPrimaryFieldType $newEntityId = saveFull${refEntityName}($refEntityField);
                    newRelations.add(($relationEntityDto) new $relationEntityDto().$sourceFieldSetter($primaryFieldName).$targetFieldSetter($newEntityId));
                    $oldEntityIds.remove($newEntityId);
                });
                $relationEntityRepo.saveAll(newRelations);
            }
            if (!$oldEntityIds.isEmpty()) {
                // delete old relations, but not $entityList because of ManyToMany relation
                $relationEntityRepo.deleteAllById($oldEntityIds.stream().map(id -> Pair.of($primaryFieldName, id)).toList());
            }""".parseAsBlock()
        ).setJavadocComment(
            """Save property {@link $paramType#$fieldGetter()} of ManyToMany relation.

@param $paramName full $entityName dto
@param $primaryFieldName id of $entityName"""
        )
        return method
    }

    private fun addSaveFullEntityMethods(
        clazz: JavaClassNode,
        entity: EntityElem,
        entitiesMap: EntitiesMap
    ): JavaMethodNode {
        // if this entity is OneToMany target to this entity
        val oneToManySyntheticField = entitiesMap.findOneToManySyntheticField(entity)
        val entityName = entity.name
        val methodName = "saveFull$entityName"
        val fullDtoName = entity.getFullDtoName()
        val fullDtoParam = fullDtoName.lowerFirst()

        // Long saveFullOrder(final OrderFullDto orderFullDto, final Long itemOrdersId) {}
        val method = clazz.addMethod(methodName)
            .addAnnotation(NotNull::class.java)
            .addParameter(fullDtoName, fullDtoParam)
            .setReturnType(entity.getPrimaryField().asJavaType())

        if (oneToManySyntheticField != null) {
            method.addParameter(oneToManySyntheticField.asJavaType(), oneToManySyntheticField.name)
            /*
            Long saveFullOrder(final OrderFullDto orderFullDto) {
                return saveFullOrder(orderFullDto, null);
            }
            */
            clazz.addMethod(methodName)
                .addAnnotation(NotNull::class.java)
                .addParameter(fullDtoName, fullDtoParam)
                .setReturnType(entity.getPrimaryField().asJavaType())
                .setBody("return $methodName($fullDtoParam, null);".parseAsStatement())
        }

        return method
    }

    private fun addInnerContextClass(clazz: JavaClassNode, entities: List<EntityElem>): JavaClassNode {
        val contextClass = clazz.addInnerClass("InnerContext").implementInterface(AutoCloseable::class.java)

        entities.filter { it.isNotSynthetic() }.forEach { entity ->
            // private final Set<Long> itemIdsToDelete = new HashSet<>(0);
            val entityField = entity.name.lowerFirst()
            contextClass.addField("Set<Long>", "${entityField}IdsToDelete")
                .withModifiers(Modifier.Keyword.FINAL)
                .setInitializer("new HashSet<>(0)")
            // private final Set<Long> itemIdsSaved = new HashSet<>(0);
            contextClass.addField("Set<Long>", "${entityField}IdsSaved")
                .withModifiers(Modifier.Keyword.FINAL)
                .setInitializer("new HashSet<>(0)")
        }

        contextClass.addField("EntityUpdateType", "updateType", true)
            .withModifiers(Modifier.Keyword.FINAL)
        return contextClass
    }

    private fun addPublicSaveFullEntityMethods(clazz: JavaClassNode, entity: EntityElem) {
        /*
        public Long saveFullOrder(final OrderFullDto orderFullDto) {
            return saveFullOrder(orderFullDto, EntityUpdateType.DEFAULT);
        }
        */
        val entityName = entity.name
        val methodName = "saveFull$entityName"
        val fullDtoName = entity.getFullDtoName()
        val fullDtoParam = fullDtoName.lowerFirst()
        val entityIdType = entity.getPrimaryFieldType()
        clazz.addMethod(methodName)
            .addAnnotation(NotNull::class.java)
            .withModifiers(Modifier.Keyword.PUBLIC)
            .addParameter(fullDtoName, fullDtoParam)
            .setReturnType(entityIdType)
            .setBody("return saveFull$entityName($fullDtoParam, EntityUpdateType.DEFAULT);".parseAsStatement())
            .setJavadocComment(
                """Save {@link $fullDtoName} with all inner relations.

@param $fullDtoParam full $entityName dto
@return id of saved $entityName"""
            )

        /*
        public Long saveFullOrder(final OrderFullDto orderFullDto, EntityUpdateType updateType) {
            try (final InnerContext ctx = new InnerContext(updateType)) {
                return ctx.saveFullOrder(orderFullDto);
            }
        }
        */
        val updateTypeParam = "updateType"
        clazz.addMethod(methodName)
            .addAnnotation(NotNull::class.java)
            .withModifiers(Modifier.Keyword.PUBLIC)
            .addParameter(fullDtoName, fullDtoParam)
            .addParameter("EntityUpdateType", updateTypeParam)
            .setReturnType(entityIdType)
            .setBody(
                """try (final InnerContext ctx = new InnerContext($updateTypeParam)) {
            return ctx.$methodName($fullDtoParam);
            }""".parseAsBlock()
            )
            .setJavadocComment(
                """Save {@link $fullDtoName} with all inner relations.

@param $fullDtoParam full $entityName dto
@param $updateTypeParam update type
@return id of saved $entityName"""
            )
    }

    private fun addFields(
        clazz: JavaClassNode,
        entity: EntityElem,
        fields: MutableList<JavaFieldNode>
    ) {
        val repositoryName = entity.getRepositoryName()

        val repoField = clazz.addField(repositoryName, repositoryName.lowerFirst())
            .setModifiers(Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL)
        fields.add(repoField)
        clazz.addImportBySimpleName(entity.getDtoName())

        if (entity.isNotSynthetic()) {
            val mapperName = entity.getMapperName()
            val mapperField = clazz.addField(mapperName, mapperName.lowerFirst())
                .setModifiers(Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL)
            fields.add(mapperField)
            clazz.addImportBySimpleName(mapperName)
        }
    }

    private fun addCloseMethod(contextClass: JavaClassNode, entities: List<EntityElem>) {
        val method = contextClass.addMethod("close")
            .withModifiers(Modifier.Keyword.PUBLIC)
            .addAnnotation(Override::class.java)
        val statements = NodeList<Statement>()
        val entities = entities.filter { it.isNotSynthetic() }

        entities.forEach { entity ->
            // itemIdsToDelete.removeAll(itemIdsSaved);
            val entityField = entity.name.lowerFirst()
            val idsToDelete = "${entityField}IdsToDelete"
            val idsSaved = "${entityField}IdsSaved"
            statements.add("$idsToDelete.removeAll($idsSaved);".parseAsStatement())
        }
        statements.first.ifPresent { it.setLineComment(" preserve saved entities from deletion") }
        val firstDeleteStatementIndex = statements.size
        entities.forEach { entity ->
            // itemRepository.deleteAllById(itemIdsToDelete);
            val entityField = entity.name.lowerFirst()
            val idsToDelete = "${entityField}IdsToDelete"
            val idsSaved = "${entityField}IdsSaved"
            val entityRepo = entity.getRepositoryName().lowerFirst()
            statements.add("$entityRepo.deleteAllById($idsToDelete);".parseAsStatement())
            statements.add("$idsToDelete.clear();".parseAsStatement())
            statements.add("$idsSaved.clear();".parseAsStatement())
        }
        if (firstDeleteStatementIndex < statements.size) {
            statements[firstDeleteStatementIndex].setLineComment(" delete entities")
        }

        method.setBody(statements)
    }

    private fun addEntityUpdateType(repositoryDir: JavaDirectoryNode, packagePath: Any, context: GeneratorContext) {
        val values = mapOf("package" to packagePath)
        val idTracerFile = repositoryDir.addJavaFile("EntityUpdateType.java")
        context.writeTemplate(idTracerFile, "/templates/jooq/EntityUpdateType.java.vm", values)
    }

    private fun EntityElem.getLoadEntityIdFieldsOrReturnMethod(): String = "load${this.name}IdFieldsOrReturn"

    private companion object {
        val log = LoggerFactory.getLogger(EntityGraphStoreFeature::class.java)!!
    }
}
