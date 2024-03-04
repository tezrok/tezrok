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
import lombok.extern.slf4j.Slf4j
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

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
            .addAnnotation(Slf4j::class.java)
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
                addTryGetEntityIdMethod(contextClass, entity)
            }

        entities.filter { it.isNotSynthetic() }
            .forEach { entity ->
                addDeleteOldEntitiesMethod(contextClass, entity, entitiesMap)
            }

        entities.filter { it.isNotSynthetic() }
            .forEach { entity ->
                addHasOnlyUniqueFieldsMethod(contextClass, entity)
            }

        addCloseMethod(contextClass, entities)
    }

    private fun addTryGetEntityIdMethod(clazz: JavaClassNode, entity: EntityElem) {
        val fullDtoName = entity.getFullDtoName()
        val entityIdType = entity.getPrimaryFieldType()
        val dtoName = entity.getDtoName()
        val statements = NodeList<Statement>()
        val method = clazz.addMethod(entity.getTryGetEntityIdMethod())
            .addParameter(fullDtoName, "newFullDto")
            .setReturnType(entityIdType)
            .setJavadocComment("Return primary id if we need just return it, otherwise null if we need save dto.")

        val entityField = entity.name.lowerFirst()
        val entityIdsSaving = """${entityField}IdsSaving"""
        val entityRepository = entity.getRepositoryName().lowerFirst()
        val primaryField = entity.getPrimaryField()
        val hasOneToOneRelations = entity.hasRelations(EntityRelation.OneToOne)
        if (hasOneToOneRelations) {
            val idFieldsByPrimaryIdMethod = entity.getGetIdFieldsByPrimaryId();

            statements.addAll(
                """if (newFullDto.getId() != null) {
                final $dtoName oldIdFields = $entityRepository.$idFieldsByPrimaryIdMethod(newFullDto.getId(), $dtoName.class);
                if (oldIdFields != null) {
                    deleteOldEntities(newFullDto, oldIdFields);
                }
                
                // need to update dto by id
                return null;
            }""".parseAsStatements()
            )
        } else {
            statements.addAll(
                """if (newFullDto.getId() != null) {
                // need to update dto by id
                return null;
            }""".parseAsStatements()
            )
        }

        val primaryIdSetter = primaryField.getSetterName()
        val uniqueGroups = entity.getUniqueGroups(true)
        val uniqueFields = entity.getUniqueFields()
        if (uniqueFields.isNotEmpty() || uniqueGroups.isNotEmpty()) {
            var code = "if (updateType == EntityUpdateType.UPDATE_RELATION_BY_NAME) {\n// check unique fields\n"

            if (uniqueFields.isNotEmpty()) {
                uniqueFields.forEach { field ->
                    val idFieldsByUniqField = entity.getGetIdFieldsByUniqueField(field)
                    val fieldGetter = field.getGetterName()
                    // case when we have uniq field and id fields
                    if (hasOneToOneRelations) {
                        code += """
                if (newFullDto.$fieldGetter() != null) {
                    // if we have unique field, we can load id fields by it
                    final $dtoName oldIdFields = $entityRepository.$idFieldsByUniqField(newFullDto.$fieldGetter(), $dtoName.class);
                    if (oldIdFields != null) {
                        newFullDto.$primaryIdSetter(oldIdFields.getId());
                        if ($entityIdsSaving.contains(oldIdFields.getId())) {
                            return oldIdFields.getId();
                        }
                        deleteOldEntities(newFullDto, oldIdFields);
                        if (hasOnlyUniqueFields(newFullDto)) {
                            // if we don't have id, have only unique fields and need update only relation, return only id
                            return oldIdFields.getId();
                        }
                        return null;
                    }
                }"""
                    } else {
                        val primaryFieldByUniqField = entity.getGetPrimaryIdFieldByUniqueField(field)
                        code += """
                if (newFullDto.$fieldGetter() != null) {
                    // if we have unique field, we can load id fields by it
                    final $entityIdType primaryId = $entityRepository.$primaryFieldByUniqField(newFullDto.$fieldGetter());
                    if (primaryId != null) {
                        newFullDto.$primaryIdSetter(primaryId);
                        if ($entityIdsSaving.contains(primaryId)) {
                            return primaryId;
                        }
                        if (hasOnlyUniqueFields(newFullDto)) {
                            // if we don't have id, have only unique fields and need update only relation, return only id
                            return primaryId;
                        }
                        return null;
                    }
                }"""
                    }
                }
            }

            uniqueGroups.forEach { (_, fields) ->
                val notNullCondition = fields.joinToString(separator = "&&")
                { field -> "newFullDto.${field.getGetterName()}() != null" }
                val argumentList = fields.joinToString { field -> fieldArgumentGetter(field) }

                if (hasOneToOneRelations) {
                    val methodName = entity.getGetIdFieldsByGroupFields(fields)
                    code += """
                    if ($notNullCondition) {
                        final $dtoName oldIdFields = $entityRepository.$methodName($argumentList, $dtoName.class);
                        if (oldIdFields != null) {
                            newFullDto.$primaryIdSetter(oldIdFields.getId());
                            if ($entityIdsSaving.contains(oldIdFields.getId())) {
                                return oldIdFields.getId();
                            }
                            deleteOldEntities(newFullDto, oldIdFields);
                            if (hasOnlyUniqueFields(newFullDto)) {
                                // if we don't have id, have only group unique fields and need update only relation, return only id
                                return oldIdFields.getId();
                            }
                            return null;
                        }"""
                } else {
                    val methodName = entity.getGetPrimaryIdFieldByGroupFields(fields)
                    code += """
                    if ($notNullCondition) {
                        final $entityIdType primaryId = $entityRepository.$methodName($argumentList);
                        if (primaryId != null) {
                            newFullDto.$primaryIdSetter(primaryId);
                            if ($entityIdsSaving.contains(primaryId)) {
                                return primaryId;
                            }
                            if (hasOnlyUniqueFields(newFullDto)) {
                                // if we don't have id, have unique field and need update only relation, return only id
                                return primaryId;
                            }
                            return null;
                        }
                    }"""
                }
            }

            code += "}"
            statements.addAll(code.parseAsStatements())
        }

        statements.add("return null;".parseAsStatement())
        method.setBody(statements)
    }

    private fun fieldArgumentGetter(field: FieldElem): String {
        val getter = "newFullDto.${field.getGetterName()}()"
        return if (field.isLogic()) "$getter.getId()" else getter
    }

    private fun addHasOnlyUniqueFieldsMethod(contextClass: JavaClassNode, entity: EntityElem) {
        val uniqGroups = entity.getUniqueGroups(true)
        val uniqueFields = entity.getUniqueFields()
        val uniqGroupFields = uniqGroups.flatMap { it.value }
        val otherFields = entity.fields.filter { field -> field !in uniqueFields && field !in uniqGroupFields }
        contextClass.addImport(StringUtils::class.java)
            .addImport(Stream::class.java)
            .addImport(Objects::class.java)

        val uniqStatement = uniqueFields.filter { it.isNotSynthetic() }
            .joinToString { field -> "fullDto.${field.getGetterName()}()" }
            .let { str -> if (str.isNotBlank()) "Stream.of($str).anyMatch(Objects::nonNull)" else "" }
        val groupsStatement = uniqGroups.map { (_, fields) ->
            fields.joinToString(separator = "&&")
            { field -> "fullDto.${field.getGetterName()}() != null" }
        }
        val uniqStatements = (groupsStatement + uniqStatement).filter { it.isNotBlank() }
            .joinToString(" || ").let { if (it.isNotBlank()) "($it)" else "" }
        val otherStatement = otherFields.filter { p -> p.isNotSynthetic() && p.isNotPrimaryField() }
            .joinToString { field -> "fullDto.${field.getGetterName()}()" }
            .let { str -> if (str.isNotBlank()) "Stream.of($str).allMatch(Objects::isNull)" else "" }
        val finalStatement =
            if (uniqStatements.isNotBlank()) listOf(uniqStatements, otherStatement)
                .filter { it.isNotBlank() }
                .joinToString(
                    " && ",
                    prefix = "return ",
                    postfix = ";"
                ) else "return false;"

        contextClass.addMethod(entity.getHasOnlyUniqueFieldsMethod())
            .addParameter(entity.getFullDtoName(), "fullDto")
            .setReturnType("boolean")
            .setBody(finalStatement.parseAsStatement())
            .setJavadocComment("Returns true if at least one of the unique field is not null and other fields are null.")
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
        val entityField = entity.name.lowerFirst()

        statements.add("final $idType fullDtoId = $fullDtoParam.getId();".parseAsStatement());
        statements.add(
            """if (fullDtoId != null) {
                // avoid infinite recursion
                if (${entityField}IdsSaving.contains(fullDtoId)) {
                    return fullDtoId;
                }
                ${entityField}IdsSaving.add(fullDtoId); 
                ${entityField}IdsSaved.add(fullDtoId); 
                }""".parseAsStatement()
        )

        // generates save OneToOne and ManyToOne fields
        entity.fields.filter { field -> field.relation == EntityRelation.ManyToOne || field.relation == EntityRelation.OneToOne }
            .forEach { field ->
                val refEntity = entitiesMap.getRefEntity(field)
                if (field.relation == EntityRelation.OneToOne) {
                    // saveOneToOneOrder(orderFullDto.getNextOrder());
                    val methodName = getOrAddOneToOneMethod(refEntity, method.getOwner()).getName()
                    statements.add(
                        "$methodName($fullDtoParam.${field.getGetterName()}());"
                            .parseAsStatement()
                            .withLineComment(" save property \"${field.name}\" - OneToOne relation")
                    )
                } else if (field.relation == EntityRelation.ManyToOne) {
                    // orderDtoPre.setSelectedItemId(saveManyToOneItem(orderFullDto.getSelectedItem()));
                    val methodName = getOrAddManyToOneMethod(refEntity, method.getOwner()).getName()
                    statements.add(
                        "$methodName($fullDtoParam.${field.getGetterName()}());"
                            .parseAsStatement()
                            .withLineComment(" save property \"${field.name}\" - ManyToOne relation")
                    )
                }
            }

        // Long id = tryGetOrderId(orderFullDto);
        val loadMethod = entity.getTryGetEntityIdMethod()
        statements.add(
            "final $idType id = $loadMethod($fullDtoParam);".parseAsStatement()
                .withLineComment(" stop save if id found")
        )

        // if (id != null) { return id; }
        statements.add(
            """if (id != null) {
            ${entityField}IdsSaving.remove(id); return id; }""".parseAsStatement()
        )

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

        // final OrderDto orderDto = orderRepository.save(orderDtoPre);
        val entityRepositoryName = entity.getRepositoryName().lowerFirst()
        statements.add(
            "final $dtoName $dtoField = $entityRepositoryName.save($dtoPreField);"
                .parseAsStatement().withLineComment(" save $entityName")
        )

        // orderIdsSaved.add(orderDto.getId());
        // orderIdsSaving.add(orderDto.getId());
        val dtoPrimaryFieldCall = "$dtoField.${primaryField.getGetterName()}()"
        statements.add("${entityField}IdsSaved.add($dtoPrimaryFieldCall);".parseAsStatement())
        statements.add(
            "${entityField}IdsSaving.add($dtoPrimaryFieldCall);"
                .parseAsStatement()
                .withLineComment(" preserve $entityName from deletion and to avoid infinite recursion")
        )

        entity.fields.filter { field -> field.relation == EntityRelation.OneToMany || field.relation == EntityRelation.ManyToMany }
            .forEach { field ->
                // saveOrderItems(orderFullDto, orderDto.getId());
                val methodName = addToManyMethod(field, entity, entitiesMap, method.getOwner()).getName()
                statements.add(
                    "$methodName($fullDtoParam, $dtoPrimaryFieldCall);"
                        .parseAsStatement()
                        .withLineComment(" save property \"${field.name}\" - ${field.relation} relation")
                )
            }

        // orderFullDto.setId(orderDto.getId());
        statements.add("$fullDtoParam.${primaryField.getSetterName()}($dtoPrimaryFieldCall);".parseAsStatement())

        // orderIdsSaving.remove(orderDto.getId());
        statements.add(
            "${entityField}IdsSaving.remove($dtoPrimaryFieldCall);".parseAsStatement()
        )
        // return orderDto.getId();
        statements.add("return $dtoPrimaryFieldCall;".parseAsStatement())

        method.setBody(statements)
    }

    private fun getOrAddManyToOneMethod(entity: EntityElem, clazz: JavaClassNode): JavaMethodNode {
        val methodName = "saveManyToOne${entity.name}"
        clazz.getMethod(methodName)?.let { return it }

        val entityName = entity.name
        val paramName = "new$entityName"
        val paramType = entity.getFullDtoName()
        return clazz.addMethod(methodName)
            .addParameter(paramType, paramName)
            .setBody("if ($paramName != null) {saveFull$entityName($paramName);}".parseAsBlock())
            .setJavadocComment(
                """Save property of type {@link $paramType} and ManyToOne relation.

@param $paramName $entityName to save"""
            )
    }

    private fun getOrAddOneToOneMethod(entity: EntityElem, clazz: JavaClassNode): JavaMethodNode {
        val methodName = "saveOneToOne${entity.name}"
        clazz.getMethod(methodName)?.let { return it }

        val entityName = entity.name
        val paramName = "new$entityName"
        val paramType = entity.getFullDtoName()
        return clazz.addMethod(methodName)
            .addParameter(paramType, paramName)
            .setBody("if ($paramName != null) {saveFull$entityName($paramName);}".parseAsBlock())
            .setJavadocComment(
                """Save property of type {@link $paramType} and OneToOne relation.

 @param $paramName new $entityName to save"""
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
                    if ($oldEntityIds.contains($newEntityId)) {
                        $oldEntityIds.remove($newEntityId);
                    } else {
                        newRelations.add(($relationEntityDto) new $relationEntityDto().$sourceFieldSetter($primaryFieldName).$targetFieldSetter($newEntityId));
                    }
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

    private fun addDeleteOldEntitiesMethod(contextClass: JavaClassNode, entity: EntityElem, entitiesMap: EntitiesMap) {
        val oneToOneFields = entity.fields.filter { it.relation == EntityRelation.OneToOne }
        if (oneToOneFields.isEmpty()) {
            return
        }

        val statements = NodeList<Statement>()
        val fullDtoName = entity.getFullDtoName()
        val fullDtoParam = fullDtoName.lowerFirst()
        val dtoName = entity.getDtoName()
        val dtoParam = "oldIdFields"

        /*
            // delete old BookProfile as it's not used anymore and it's OneToOne relation
            if (oldIdFields.getProfileId() != null && (bookFullDto.getProfile() == null || oldIdFields.getProfileId().equals(bookFullDto.getProfile().getBookProfileId()))) {
                bookProfileIdsToDelete.add(oldIdFields.getProfileId());
            }
         */
        oneToOneFields.forEach { field ->
            val fieldGetter = field.getGetterName()
            val syntheticField = entitiesMap.getSyntheticField(entity, field)
            val targetEntity = entitiesMap.getRefEntity(field)
            val targetEntityName = targetEntity.name.lowerFirst()
            val targetEntityIdGetter = targetEntity.getPrimaryField().getGetterName()
            val syntheticGetter = syntheticField.getGetterName()
            statements.add(
                """if (oldIdFields.$syntheticGetter() != null && ($fullDtoParam.$fieldGetter() == null || !oldIdFields.$syntheticGetter().equals($fullDtoParam.$fieldGetter().$targetEntityIdGetter()))) {
                ${targetEntityName}IdsToDelete.add(oldIdFields.$syntheticGetter());
            }""".parseAsStatement()
                    .withLineComment(" delete old ${targetEntity.name} as it's not used anymore and it's OneToOne relation")
            )
        }

        // private void deleteOldEntities(@NotNull BookFullDto bookFullDto, @NotNull BookDto oldIdFields)
        contextClass.addMethod("deleteOldEntities")
            .addParameter(fullDtoName, fullDtoParam)
            .addParameter(dtoName, dtoParam)
            .setBody(statements)
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
            // private final Set<Long> itemIdsSaving = new HashSet<>(0);
            contextClass.addField("Set<Long>", "${entityField}IdsSaving")
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
            } catch (final Exception ex) {
                log.error("Save SubjectFullDto failed: {}", subjectFullDto);
                throw new IllegalStateException("Save SubjectFullDto failed", ex);
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
            } catch (final Exception ex) {
            log.error("Save $fullDtoName failed: {}", $fullDtoParam);
            throw new IllegalStateException("Save $fullDtoName failed", ex);
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

    private fun EntityElem.getTryGetEntityIdMethod(): String = "tryGet${this.name}Id"

    private fun EntityElem.getHasOnlyUniqueFieldsMethod(): String = "hasOnlyUniqueFields"


    private companion object {
        val log = LoggerFactory.getLogger(EntityGraphStoreFeature::class.java)!!
    }
}
