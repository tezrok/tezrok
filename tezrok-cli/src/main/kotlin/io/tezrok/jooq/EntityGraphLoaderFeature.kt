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
import org.jetbrains.annotations.NotNull
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
            .setJavadocComment(
                """Loads whole entity by id with all relations.
 
@see IdTracerRoot"""
            )
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
            implementGetFullEntityByIdMethod(method, entity)
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
        val entityName = entity.name
        validateSinglePrimary(entity)
        val primaryField = entity.getPrimaryField()
        val fullDtoName = "${entityName}FullDto"
        val methodName = "getFull${entityName}sByIds"
        clazz.addMethod("getFull${entityName}ById").withModifiers(Modifier.Keyword.PUBLIC)
            .addParameter(primaryField.asJavaType(tryPrimitive = true), "id")
            .setReturnType(fullDtoName)
            .addAnnotation(Nullable::class.java)
            .setBody("return $methodName(List.of(id)).stream().findFirst().orElse(null);".parseAsStatement())
            .setJavadocComment(
                """Get {@link $fullDtoName} with all inner relations by id.

@param id primary id
@return {@link ${fullDtoName}} by id"""
            )

        val method = clazz.addMethod(methodName).withModifiers(Modifier.Keyword.PUBLIC)
            .addParameter("Collection<${primaryField.asJavaType()}>", "ids")
            .addAnnotation(NotNull::class.java)
            .setReturnType("List<$fullDtoName>")
            .setJavadocComment(
                """Get list of {@link $fullDtoName} with all inner relations by ids.

@param ids ids of {@link $fullDtoName}s
@return list of {@link $fullDtoName} by ids"""
            )

        clazz.addImportBySimpleName("${entityName}Dto")
        clazz.addImportBySimpleName(fullDtoName)

        return method
    }

    private fun implementGetFullEntityByIdMethod(
        method: JavaMethodNode,
        entity: EntityElem
    ) {
        val statements = NodeList<Statement>()
        val entityName = entity.name
        val entityField = entityName.lowerFirst()
        val fullDtoName = entity.getFullDtoName()
        val repositoryFieldName = entity.getRepositoryName().lowerFirst()

        statements.addAll(
            """final IdTracerRoot tracer = new IdTracerRoot(this::getStub);
        // phase 1: collect only ids by root ids
        final List<IdTracerRoot.IdTracer> tracers = $repositoryFieldName.listIdsByIds(ids).stream()
                .map(id -> tracer.createTracer($fullDtoName.class, id))
                .toList();
        loadIdsBy${entityName}Ids(ids, tracer);
        // phase 2: load all entities by ids collected during phase 1
        loadAllEntitiesByIds(tracer);
        // fill entity's fields by already loaded entities
        final List<$fullDtoName> orders = tracers.stream()
                .map(trc -> ($fullDtoName) trc.getInstance())
                .peek($entityField -> build${entityName}FullTree($entityField, tracer))
                .toList();
        tracer.clearAll();
        return orders;""".parseAsStatements()
        )

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
        val idTracerFile = repositoryDir.addJavaFile("IdTracerRoot.java")
        context.writeTemplate(idTracerFile, "/templates/jooq/IdTracerRoot.java.vm", values)
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
        val method = clazz.addMethod(entity.getLoadIdsByEntityIdsMethodName())
            .withModifiers(Modifier.Keyword.PROTECTED)
            .addParameter("Collection<$primaryFieldType>", entityIdsParam)
            .addParameter("IdTracerRoot", TRACER_FIELD)
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
        //            final IdTracerRoot.IdTracer entityObject = tracer.lookup(EntityFullDto.class, entity.getId());
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
            builder.append("final IdTracerRoot.IdTracer object = $TRACER_FIELD.lookup(${fullDtoName}.class, ${entityFieldName}.getId());")
            idFields.forEach { field ->
                builder.append(NEWLINE)
                val getterName = field.getGetterName()
                val refField = entitiesMap.getRefField(field)
                val refEntity = entitiesMap[refField.type!!]
                val refFullDto = refEntity.getFullDtoName()
                builder.append("object.setProperty(\"${refField.name}\", $refFullDto.class, ${entityFieldName}.$getterName());")
                allRelatedEntities[refEntity.name] = refEntity.getPrimaryField().asJavaType()
            }
            val methodName = entity.getFindIdFieldsByPrimaryIdIn()
            statements.add(
                """${repositoryFieldName}.$methodName($entityIdsParam, ${entityDtoName}.class)
                    .forEach(${entityFieldName} -> {
                        $builder
                    });""".parseAsStatement()
                    .withLineComment(" load all inner property ids inside $entityName by $entityName ids")
            )
        }

        // OneToMany relation fields
        entity.fields.filter { field -> field.relation == EntityRelation.OneToMany }
            .forEach { field ->
                // code: itemRepository.findIdItemsEntityIdByItemsEntityIdIn(entityIds, ItemDto.class).stream()
                //       .collect(groupingBy(ItemDto::getItemsEntityId))
                //       .forEach((itemsEntityId, list) -> {
                //           final IdTracerRoot.IdTracer object = tracer.lookup(EntityFullDto.class, itemsEntityId);
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
                            final IdTracerRoot.IdTracer object = $TRACER_FIELD.lookup($fullDtoName.class, $syntheticFieldName);
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
                //          final IdTracerRoot.IdTracer object = tracer.lookup(EntityFullDto.class, entityId);
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

                statements.add(
                    """$relationRepository.findBy${sourceFieldUName}In($entityIdsParam).stream()
                .collect(Collectors.groupingBy($relationEntityName::$sourceFieldGetter))
                .forEach(($entityId, list) -> {
                    final IdTracerRoot.IdTracer object = $TRACER_FIELD.lookup($fullDtoName.class, $entityId);
                    List<${targetField.asJavaType()}> ids = list.stream().map($relationEntityName::$targetFieldGetter).toList();
                    object.setListProperty("${field.name}", $refFullDto.class, ids);
                });""".parseAsStatement()
                        .withLineComment(" property \"${field.name}\" of type List<$refFullDto> with ManyToMany relation to $refFullDto")
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
            statements.add("loadIdsBy${entityName}Ids(next${entityName}Ids, tracer);".parseAsStatement())
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
            .addParameter("IdTracerRoot", TRACER_FIELD)
            .setJavadocComment("Loads all entities by ids collected by {@link IdTracerRoot}.")

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
            //            entityRepository.findAllByIds(allEntityIds).stream()
            //                .map(entityMapper::toEntityFullDto)
            //                .forEach(tracer::setObjectInstance);
            //        }
            val repositoryFieldName = entity.getRepositoryName().lowerFirst()
            val mapperFieldName = entity.getMapperName().lowerFirst()
            statements.add(
                """if (!$allEntityIdsFieldName.isEmpty()) {
                        ${repositoryFieldName}.findAllByIds($allEntityIdsFieldName).stream()
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
            .addParameter("IdTracerRoot", TRACER_FIELD)
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
        const val TRACER_FIELD = "tracer"
    }
}


