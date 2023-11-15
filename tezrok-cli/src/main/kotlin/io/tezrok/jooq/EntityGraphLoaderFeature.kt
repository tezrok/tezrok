package io.tezrok.jooq

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.ClassOrInterfaceType
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntitiesMap
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.EntityRelation
import io.tezrok.api.input.OneToManyMethod
import io.tezrok.api.java.*
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.*
import org.jetbrains.annotations.Nullable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.stream.Collectors

/**
 * Create IdTracer and EntityGraphLoader classes.
 */
internal class EntityGraphLoaderFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val projectElem = context.getProject()
            val repositoryDir = applicationPackageRoot.getOrAddJavaDirectory("repository")
            addIdTracer(repositoryDir, projectElem.packagePath, context)

            val schemaModule = context.getProject().modules.find { it.name == module.getName() }
                ?: throw IllegalStateException("Module ${module.getName()} not found")
            schemaModule.schema?.entities?.let { entities ->
                addEntityGraphLoader(repositoryDir, entities)
            }
        } else {
            log.warn("Application package root is not set")
        }

        return true
    }

    private fun addEntityGraphLoader(
        repositoryDir: JavaDirectoryNode,
        entities: List<EntityElem>
    ) {
        val clazz = repositoryDir.addClass("EntityGraphLoader")
            .addAnnotation(Service::class.java)
            .setModifiers(Modifier.Keyword.PUBLIC)
            .addImport(Collection::class.java)
            .addImport(List::class.java)
            .addImport(Set::class.java)
            .addImport(Collectors::class.java)

        // add fields which are initialized in constructor
        val fields = mutableListOf<JavaFieldNode>()
        entities.forEach { entity ->
            addFields(clazz, entity, fields)
        }
        fields.forEach(clazz::initInConstructor)

        // add public load methods
        val methods = mutableMapOf<EntityElem, JavaMethodNode>()
        entities.filter { it.isNotSynthetic() }.forEach { entity ->
            methods[entity] = addGetFullEntityByIdMethod(clazz, entity)
        }

        // add getStub method
        val entitiesMap = EntitiesMap.from(entities)
        addGetStubMethod(clazz, entitiesMap)

        // implement public load methods
        methods.forEach { (entity, method) ->
            implementGetFullEntityByIdMethod(method, entity, entitiesMap)
        }

        // add loadIdsByEntityIds methods
        entities.filter { it.isNotSynthetic() }.forEach { entity ->
            addLoadIdsByEntityIdsMethod(clazz, entity, entitiesMap)
        }

        // add loadAllEntitiesByIds methods
        addLoadAllEntitiesByIdsMethod(clazz, entitiesMap)

        entities.filter { it.isNotSynthetic() }.forEach { entity ->
            addBuildEntityFullTreeMethod(clazz, entity, entitiesMap)
        }
    }

    /**
     * Adds public method to load entity by id - `EntityFullDto getFullEntityById(ID id)`
     *
     * Note: this method only adds method declaration, implementation should be added in [implementGetFullEntityByIdMethod]
     */
    private fun addGetFullEntityByIdMethod(
        clazz: JavaClassNode,
        entity: EntityElem
    ): JavaMethodNode {
        val name = entity.name
        validateSinglePrimary(entity)
        val primaryField = entity.getPrimaryField()
        val fullDtoName = "${name}FullDto"
        val method = clazz.addMethod("getFull${name}ById").withModifiers(Modifier.Keyword.PUBLIC)
            .addParameter(primaryField.asJavaType(tryPrimitive = true), "id")
            .setReturnType(fullDtoName)
            .addAnnotation(Nullable::class.java)
            .setJavadocComment(
                """Get {@link ${fullDtoName}} with all inner relations by id.

@param id primary id
@return {@link ${fullDtoName}} by id"""
            )

        clazz.addImportBySimpleName("${name}Dto")
        clazz.addImportBySimpleName(fullDtoName)

        return method
    }

    private fun implementGetFullEntityByIdMethod(
        method: JavaMethodNode,
        entity: EntityElem,
        entitiesMap: EntitiesMap
    ) {
        val statements = NodeList<Statement>()
        val paramName = method.getParameters().map { it.getName() }.first()
        val dtoName = entity.getDtoName()
        val dtoFieldName = entity.getDtoName().lowerFirst()
        val dtoField = VariableDeclarationExpr(ClassOrInterfaceType(dtoName), dtoFieldName, Modifier.finalModifier())
        val fullDtoName = entity.getFullDtoName()
        val dtoFullFieldName = fullDtoName.lowerFirst()
        val fullDtoField =
            VariableDeclarationExpr(ClassOrInterfaceType(fullDtoName), dtoFullFieldName, Modifier.finalModifier())
        val repositoryFieldName = entity.getRepositoryName().lowerFirst()

        // code: EntityDto entityDto = entityRepository.getById(id);
        statements.add(
            JavaCallExpressionNode.ofMethodCall("$repositoryFieldName.getById")
                .addNameArgument(paramName)
                .assignToAsStatement(dtoField)
                .apply { setLineComment(" phase 1: collect only ids by root id") }
        )

        // code: if (entityDto == null) {
        //     return null;
        // }
        statements.add(
            IfStmt(
                BinaryExpr(dtoFieldName.asNameExpr(), NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
                ReturnStmt(NullLiteralExpr()).asBlock(),
                null
            )
        )

        // code: EntityFullDto entityFullDto = entityMapper.toEntityFullDto(entityDto);
        val mapperFieldName = entity.getMapperName().lowerFirst()
        statements.add(
            JavaCallExpressionNode.ofMethodCall("$mapperFieldName.to$fullDtoName")
                .addNameArgument(dtoFieldName)
                .assignToAsStatement(fullDtoField)
                .apply { setLineComment(" convert to full dto") }
        )

        // code: IdTracer tracer = new IdTracer(itemFullDto, this::getStub);
        val tracerType = ClassOrInterfaceType(TRACER)
        val tracerField = VariableDeclarationExpr(tracerType, TRACER_FIELD, Modifier.finalModifier())
        val thisGetStubRef = NameExpr("this::getStub")
        statements.add(
            ObjectCreationExpr(
                null, tracerType,
                NodeList(dtoFullFieldName.asNameExpr(), thisGetStubRef)
            ).assignToAsStatement(tracerField)
        )

        // code: tracer.setProperty("selectedItem", ItemFullDto.class, entityDto.getSelectedItemId());
        var commentAdded = false
        entity.getIdFields().filter { field -> field.external != true && field.primary != true }
            .forEach { field ->
                val getterName = field.getGetterName()
                val refField = entitiesMap.getRefField(field)
                val refEntity = entitiesMap[refField.type!!]
                val refFullDto = refEntity.getFullDtoName()
                statements.add("$TRACER_FIELD.setProperty(\"${refField.name}\", $refFullDto.class, ${dtoFieldName}.$getterName());".parseAsStatement())

                if (!commentAdded) {
                    statements.last.get().setLineComment(" set all inner property ids inside $fullDtoName")
                    commentAdded = true
                }
            }

        // code: loadIdsByEntityIds(List.of(id), tracer);
        statements.add(
            JavaCallExpressionNode.ofMethodCall(entity.getLoadIdsByEntityIdsMethodName())
                .addNameArgument("List.of($paramName)")
                .addNameArgument(TRACER_FIELD)
                .addNameArgument("true")
                .asStatement()
        )

        // code: tracer.removeFromAllIds(EntityFullDto.class, id);
        statements.add(
            JavaCallExpressionNode.ofMethodCall("$TRACER_FIELD.removeFromAllIds")
                .addNameArgument("$fullDtoName.class")
                .addNameArgument(paramName)
                .asStatement()
                .apply { setLineComment(" remove root id before loading all entities (as root entity already loaded)") }
        )

        // code: loadEntitiesByIds(tracer, id);
        statements.add(
            JavaCallExpressionNode.ofMethodCall(LOAD_ALL_ENTITIES_BY_IDS)
                .addNameArgument(TRACER_FIELD)
                .asStatement()
                .apply { setLineComment(" phase 2: load all entities by ids collected during phase 1") }
        )

        // code: buildEntityFullTree(entityFullDto, tracer);
        statements.add(
            JavaCallExpressionNode.ofMethodCall(entity.getBuildEntityFullTree())
                .addNameArgument(dtoFullFieldName)
                .addNameArgument(TRACER_FIELD)
                .asStatement()
                .apply { setLineComment(" fill entity's fields by already loaded entities") }
        )

        // code: tracer.clearAll()
        statements.add(
            JavaCallExpressionNode.ofMethodCall("$TRACER_FIELD.clearAll")
                .asStatement()
        )

        // code: return itemFullDto
        statements.add(ReturnStmt(dtoFullFieldName))
        method.setBody(statements)
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

    private fun addIdTracer(repositoryDir: JavaDirectoryNode, packagePath: String, context: GeneratorContext) {
        val values = mapOf("package" to packagePath)
        val idTracerFile = repositoryDir.addJavaFile("IdTracer.java")
        context.writeTemplate(idTracerFile, "/templates/jooq/IdTracer.java.vm", values)
    }

    private fun addGetStubMethod(clazz: JavaClassNode, entitiesMap: EntitiesMap) {
        val withIdType = "WithId<?>"
        val argName = NameExpr("object")
        val method = clazz.addImportBySimpleName("WithId")
            .addMethod("getStub")
            .withModifiers(Modifier.Keyword.PROTECTED)
            .setReturnType(withIdType)
            .setJavadocComment("Returns stub for full dto - it's a full dto without any relation fields.")
            .addParameter(withIdType, argName.nameAsString)
        val statements = NodeList<Statement>()

        // for each entity add if statement
        // if (object instanceof EntityFullDto entity) {
        //     return entityMapper.toSimpleFullDto(entity);
        // }
        entitiesMap.entities.filter { it.isNotSynthetic() }
            .forEach { entity ->
                val entityNameField = entity.name.lowerFirst()
                val entityType = entity.asType()
                val condition = InstanceOfExpr(
                    argName,
                    entityType,
                    PatternExpr(NodeList(), entityType, entityNameField.asSimpleName())
                )
                val mapperFieldName = entity.getMapperName().lowerFirst()
                val thenStmt = ReturnStmt(
                    JavaCallExpressionNode.ofMethodCall("$mapperFieldName.toSimpleFullDto")
                        .addNameArgument(entityNameField)
                        .asMethodCallExpr()
                ).asBlock()
                statements.add(IfStmt(condition, thenStmt, null))
            }

        // throw new IllegalArgumentException("Unknown type: " + object.getClass());
        statements.add(
            ThrowStmt(
                ObjectCreationExpr(
                    null,
                    ClassOrInterfaceType("IllegalArgumentException"),
                    NodeList(
                        BinaryExpr(
                            StringLiteralExpr("Unknown type: "),
                            MethodCallExpr(argName, "getClass"),
                            BinaryExpr.Operator.PLUS
                        )
                    )
                )
            )
        )

        method.setBody(statements)
    }

    private fun addLoadIdsByEntityIdsMethod(
        clazz: JavaClassNode,
        entity: EntityElem,
        entitiesMap: EntitiesMap
    ) {
        val statements = NodeList<Statement>()
        // Map<EntityName, PrimaryFieldType>
        val allRelatedEntities = mutableMapOf<String, String>()
        val entityDtoName = entity.getDtoName()
        val entityName = entity.name
        val entityFieldName = entityName.lowerFirst()
        val entityIdsParam = "${entityFieldName}Ids"
        val primaryFieldType = entity.getPrimaryField().asJavaType()
        val isRootParam = "isRoot"
        val method = clazz.addMethod(entity.getLoadIdsByEntityIdsMethodName())
            .withModifiers(Modifier.Keyword.PROTECTED)
            .addParameter("Collection<$primaryFieldType>", entityIdsParam)
            .addParameter(TRACER, TRACER_FIELD)
            .addParameter("boolean", isRootParam)
            .setJavadocComment("Load inner ids by ids of {@link ${entityDtoName}}.")

        // code:  if (entityIds.isEmpty()) { return; }
        statements.add(
            IfStmt(
                JavaCallExpressionNode.ofMethodCall("$entityIdsParam.isEmpty").asMethodCallExpr(),
                ReturnStmt().asBlock(),
                null
            )
        )

        // code: tracer.markIdsAsLoaded(EntityFullDto.class, entityIds);
        val fullDtoName = entity.getFullDtoName()
        statements.add(
            JavaCallExpressionNode.ofMethodCall("$TRACER_FIELD.markIdsAsLoaded")
                .addNameArgument("$fullDtoName.class")
                .addNameArgument(entityIdsParam)
                .asStatement()
                .apply { setLineComment(" mark $entityIdsParam as already loaded because following repo calls will get next level ids") }
        )

        //  code: entityRepository.findAllIdFieldsByPrimaryIdIn(entityIds, EntityDto.class)
        //         .forEach(entity -> {
        //            IdTracer entityObject = tracer.lookup(EntityFullDto.class, entity.getId());
        //            entityObject.setProperty("selectedItem", ItemFullDto.class, entity.getSelectedItemId());
        //            entityObject.setProperty("nextEntity", EntityFullDto.class, entity.getNextEntityId());
        //         });
        // if entity referred by OneToMany relation (field.external == true) we should skip such field
        val idFields = entity.getIdFields().filter { field -> field.external != true && field.primary != true }
        if (idFields.isNotEmpty()) {
            val repositoryFieldName = entity.getRepositoryName().lowerFirst()
            val builder =
                StringBuilder("// TODO: use special dto for this purpose instead of $entityDtoName")
            builder.append(NEWLINE)
            builder.append("$TRACER object = $TRACER_FIELD.lookup(${fullDtoName}.class, ${entityFieldName}.getId());")
            idFields.forEach { field ->
                builder.append(NEWLINE)
                val getterName = field.getGetterName()
                val refField = entitiesMap.getRefField(field)
                val refEntity = entitiesMap[refField.type!!]
                val refFullDto = refEntity.getFullDtoName()
                builder.append("object.setProperty(\"${refField.name}\", $refFullDto.class, ${entityFieldName}.$getterName());")
                allRelatedEntities[refEntity.name] = refEntity.getPrimaryField().asJavaType()
            }
            val methodName = entity.getFindAllIdFieldsByPrimaryIdIn()
            statements.add(
                IfStmt(
                    UnaryExpr(isRootParam.asNameExpr(), UnaryExpr.Operator.LOGICAL_COMPLEMENT),
                    """${repositoryFieldName}.$methodName($entityIdsParam, ${entityDtoName}.class)
                    .forEach(${entityFieldName} -> {
                        $builder
                    });""".parseAsStatement()
                        .apply { setLineComment(" load all inner property ids inside $entityName by $entityName ids") }
                        .asBlock(),
                    null
                )
            )
        }

        // OneToMany relation fields
        entity.fields.filter { field -> field.relation == EntityRelation.OneToMany }
            .forEach { field ->
                // code: itemRepository.findIdItemsEntityIdByItemsEntityIdIn(entityIds, ItemDto.class).stream()
                //       .collect(groupingBy(ItemDto::getItemsEntityId))
                //       .forEach((itemsEntityId, list) -> {
                //           IdTracer object = tracer.lookup(EntityFullDto.class, itemsEntityId);
                //           List<ID> ids = list.stream().map(ItemDto::getId).toList();
                //           object.setListProperty("items", ItemFullDto.class, ids);
                //       });
                val refEntity = entitiesMap[field.type!!]
                val refEntityRepository = refEntity.getRepositoryName().lowerFirst()
                val refFullDto = refEntity.getFullDtoName()
                val refEntityDtoName = refEntity.getDtoName()
                val refPrimaryFieldType = refEntity.getPrimaryField().asJavaType()
                val syntheticField = entitiesMap.getSyntheticField(entity, field)
                val syntheticFieldName = syntheticField.name
                val refFieldNameGetter = syntheticField.getGetterName()
                val methodName = entitiesMap.getMethodByField(
                    entity,
                    field,
                    OneToManyMethod.FindRefIdFieldsByRefSyntheticFields
                ).keys.first()
                statements.add(
                    """$refEntityRepository.$methodName($entityIdsParam, $refEntityDtoName.class).stream()
                        .collect(Collectors.groupingBy($refEntityDtoName::$refFieldNameGetter))
                        .forEach(($syntheticFieldName, list) -> {
                            // TODO: replace with special dto for this purpose instead of $refEntityDtoName
                            $TRACER object = $TRACER_FIELD.lookup($fullDtoName.class, $syntheticFieldName);
                            List<$refPrimaryFieldType> ids = list.stream().map($refEntityDtoName::getId).toList();
                            object.setListProperty("${field.name}", $refFullDto.class, ids);
                        });""".parseAsStatement()
                        .apply { setLineComment(" property \"${field.name}\" of type List<$refFullDto> with OneToMany relation to $refFullDto") }
                )
                allRelatedEntities[refEntity.name] = refPrimaryFieldType
            }

        // ManyToMany relation fields
        entity.fields.filter { field -> field.relation == EntityRelation.ManyToMany }
            .forEach { field ->
                // code: entityItemOtherItemsRepository.findByEntityIdIn(entityIds).stream()
                //      .collect(groupingBy(EntityItemOtherItems::getEntityId))
                //      .forEach((entityId, list) -> {
                //          IdTracer object = tracer.lookup(EntityFullDto.class, entityId);
                //          List<ID> ids = list.stream().map(EntityItemOtherItems::getItemId).toList();
                //          object.setListProperty("otherItems", ItemFullDto.class, ids);
                // });
                val refEntity = entitiesMap[field.type!!]
                val refFullDto = refEntity.getFullDtoName()
                val relationEntity = entitiesMap.getRelationEntity(entity, field)
                val relationEntityName = relationEntity.getDtoName()
                val sourceField = relationEntity.fields[0]
                val sourceFieldGetter = sourceField.getGetterName()
                val sourceFieldUName = sourceField.name.upperFirst()
                val targetField = relationEntity.fields[1]
                val targetFieldGetter = targetField.getGetterName()
                val relationRepository = entitiesMap.getRelationRepository(entity, field).lowerFirst()
                val entityId = "${entityFieldName}Id"

                statements.add("""$relationRepository.findBy${sourceFieldUName}In($entityIdsParam).stream()
                .collect(Collectors.groupingBy($relationEntityName::$sourceFieldGetter))
                .forEach(($entityId, list) -> {
                    $TRACER object = $TRACER_FIELD.lookup($fullDtoName.class, $entityId);
                    List<${targetField.asJavaType()}> ids = list.stream().map($relationEntityName::$targetFieldGetter).toList();
                    object.setListProperty("${field.name}", $refFullDto.class, ids);
                });""".parseAsStatement()
                    .apply { setLineComment(" property \"${field.name}\" of type List<$refFullDto> with ManyToMany relation to $refFullDto") }
                )
                allRelatedEntities[refEntity.name] = refEntity.getPrimaryField().asJavaType()
            }

        // code: Set<Long> nextEntityIds = tracer.getNewIdsByType(EntityFullDto.class);
        //        loadIdsByEntityIds(nextEntityIds, tracer);
        var commentAdded = false
        allRelatedEntities.forEach { (entityName, primaryFieldType) ->
            statements.add("Set<$primaryFieldType> next${entityName}Ids = tracer.getNewIdsByType(${entityName}FullDto.class);".parseAsStatement())
            if (!commentAdded) {
                statements.last.get().setLineComment(" load ids of the next level")
                commentAdded = true
            }
            statements.add("loadIdsBy${entityName}Ids(next${entityName}Ids, tracer, false);".parseAsStatement())
        }

        method.setBody(statements)
    }

    private fun addLoadAllEntitiesByIdsMethod(
        clazz: JavaClassNode,
        entitiesMap: EntitiesMap
    ) {
        val statements = NodeList<Statement>()
        val method = clazz.addMethod(LOAD_ALL_ENTITIES_BY_IDS)
            .withModifiers(Modifier.Keyword.PROTECTED)
            .addParameter(TRACER, TRACER_FIELD)
            .setJavadocComment("Loads all entities by ids collected by {@link IdTracer}.")

        entitiesMap.entities.filter { it.isNotSynthetic() }.forEach { entity ->
            val name = entity.name
            val fullDtoName = "${name}FullDto"
            val primaryFieldType = entity.getPrimaryField().asJavaType()

            // code: Set<ID> allEntityIds = tracer.getAllIds(EntityFullDto.class);
            val allEntityIdsFieldName = "all${entity.name}Ids"
            val allEntityIdsField = VariableDeclarationExpr(
                ClassOrInterfaceType("Set<$primaryFieldType>"),
                allEntityIdsFieldName,
                Modifier.finalModifier()
            )
            statements.add(
                JavaCallExpressionNode.ofMethodCall("$TRACER_FIELD.getAllIds")
                    .addNameArgument("$fullDtoName.class")
                    .assignToAsStatement(allEntityIdsField)
            )
            // code:   if (!allEntityIds.isEmpty()) {
            //            entityRepository.findAllById(allEntityIds).stream()
            //                .map(entityMapper::toEntityFullDto)
            //                .forEach(tracer::setObjectInstance);
            //        }
            val repositoryFieldName = entity.getRepositoryName().lowerFirst()
            val mapperFieldName = entity.getMapperName().lowerFirst()
            statements.add(
                """if (!$allEntityIdsFieldName.isEmpty()) {
                        ${repositoryFieldName}.findAllById($allEntityIdsFieldName).stream()
                        .map(${mapperFieldName}::to$fullDtoName)
                        .forEach($TRACER_FIELD::setObjectInstance);
                    }""".parseAsStatement()
            )
        }

        method.setBody(statements)
    }

    private fun addBuildEntityFullTreeMethod(
        clazz: JavaClassNode,
        entity: EntityElem,
        entitiesMap: EntitiesMap
    ) {
        val fullDtoName = entity.getFullDtoName()
        val paramName = fullDtoName.lowerFirst()
        val method = clazz.addMethod(entity.getBuildEntityFullTree())
            .withModifiers(Modifier.Keyword.PROTECTED)
            .addParameter(fullDtoName, paramName)
            .addParameter(TRACER, TRACER_FIELD)
            .setJavadocComment("Fill {@link $fullDtoName}'s fields by already loaded entities.")

        val logicFields = entity.fields.filter { it.logicField == true }
        if (logicFields.isEmpty()) return

        val statements = NodeList<Statement>()
        val entityName = entity.name
        val entityFieldName = entityName.lowerFirst()
        logicFields.forEach { field ->
            val propertyType = entitiesMap[field.type!!].name
            val propertyClass = "${propertyType}FullDto.class"
            val entityFullTreeMethod = "build${propertyType}FullTree"
            val fieldSetter = field.getSetterName()
            val fieldGetter = field.getGetterName()
            // if property is list
            if (field.relation == EntityRelation.OneToMany || field.relation == EntityRelation.ManyToMany) {
                // code: tracer.getObjectListProperty(entityFullDto, "items", ItemFullDto.class, entityFullDto::setItems);
                statements.add("$TRACER_FIELD.getObjectListProperty($paramName, \"${field.name}\", $propertyClass, $paramName::$fieldSetter);".parseAsStatement())
                // code: if (entityFullDto.getItems() != null) {
                //         entityFullDto.getItems().forEach(item -> buildItemFullTree(item, tracer));
                statements.add(
                    IfStmt(
                        BinaryExpr(
                            JavaCallExpressionNode.ofMethodCall("$paramName.$fieldGetter")
                                .asMethodCallExpr(),
                            NullLiteralExpr(),
                            BinaryExpr.Operator.NOT_EQUALS
                        ),
                        "$paramName.$fieldGetter().forEach($entityFieldName -> $entityFullTreeMethod($entityFieldName, $TRACER_FIELD));"
                            .parseAsStatement().asBlock(),
                        null
                    )
                )
            } else {
                // code : tracer.getObjectProperty(entityFullDto, "selectedItem", ItemFullDto.class, entityFullDto::setSelectedItem);
                statements.add("$TRACER_FIELD.getObjectProperty($paramName, \"${field.name}\", $propertyClass, $paramName::$fieldSetter);".parseAsStatement())
                // code:  if (entityFullDto.getSelectedItem() != null) {
                //          buildEntityFullTree(entityFullDto.getSelectedItem(), tracer);
                statements.add(
                    IfStmt(
                        BinaryExpr(
                            JavaCallExpressionNode.ofMethodCall("$paramName.$fieldGetter")
                                .asMethodCallExpr(),
                            NullLiteralExpr(),
                            BinaryExpr.Operator.NOT_EQUALS
                        ),
                        JavaCallExpressionNode.ofMethodCall(entityFullTreeMethod)
                            .addNameArgument("$paramName.$fieldGetter()")
                            .addNameArgument(TRACER_FIELD)
                            .asBlock(),
                        null
                    )
                )
            }
        }

        // code: tracer.endBuild(entityFullDto);
        statements.add(
            JavaCallExpressionNode.ofMethodCall("$TRACER_FIELD.endBuild")
                .addNameArgument(paramName)
                .asStatement()
        )

        // code  if (tracer.startBuild(entityFullDto)) {
        method.setBody(
            IfStmt(
                JavaCallExpressionNode.ofMethodCall("tracer.startBuild")
                    .addNameArgument(paramName)
                    .asMethodCallExpr(),
                BlockStmt(statements),
                null
            )
        )
    }

    private fun validateSinglePrimary(entity: EntityElem) {
        check(entity.getPrimaryFieldCount() == 1) { "Entity ${entity.name} expected have exactly one primary field" }
    }

    private fun EntityElem.getLoadIdsByEntityIdsMethodName(): String = "loadIdsBy${name}Ids"

    private fun EntityElem.getBuildEntityFullTree(): String = "build${name}FullTree"

    private companion object {
        val log = LoggerFactory.getLogger(EntityGraphLoaderFeature::class.java)!!
        val NEWLINE = System.lineSeparator()!!
        const val LOAD_ALL_ENTITIES_BY_IDS = "loadAllEntitiesByIds"
        const val TRACER = "IdTracer"
        const val TRACER_FIELD = "tracer"
    }
}


