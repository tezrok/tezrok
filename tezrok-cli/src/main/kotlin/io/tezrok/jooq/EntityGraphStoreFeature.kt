package io.tezrok.jooq

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.stmt.Statement
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntitiesMap
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.EntityRelation
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.java.JavaFieldNode
import io.tezrok.api.java.JavaMethodNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.*
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.function.UnaryOperator
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
            .addImport(Collection::class.java)
            .addImport(List::class.java)
            .addImport(Set::class.java)
            .addImport(HashSet::class.java)
            .addImport(Collectors::class.java)
            .addImport(UnaryOperator::class.java)
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
        val methods = mutableMapOf<EntityElem, JavaMethodNode>()
        entities.filter { it.isNotSynthetic() }.forEach { entity ->
            methods[entity] = addSaveFullEntityMethods(contextClass, entity)
        }

        // implement saveFullEntity methods
        val entitiesMap = EntitiesMap.from(entities)
        methods.forEach { (entity, method) ->
            implementSaveFullEntityMethod(method, entity, entitiesMap)
        }

        entities.filter { it.isNotSynthetic() }
            .forEach { entity ->
                addLoadEntityIdFieldsOrReturnMethod(contextClass, entity)
            }
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
        if (entity.getIdFields().size > 1) {
            statements.addAll(
                """if (newFullDto.getId() != null) {
                if ($entityIdsSaved.contains(newFullDto.getId())) {
                    return Pair.of(newFullDto.getId(), null);
                }
                $entityIdsSaved.add(newFullDto.getId());

                return Pair.of(null, $entityRepository.getIdFieldsById(newFullDto.getId(), $dtoName.class));
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

        val uniqueFields = entity.getUniqueStringFields()

        if (uniqueFields.isNotEmpty()) {
            var second = false
            var code = "// check unique fields\n"
            uniqueFields.forEach { field ->
                val idFieldsByUniqField = entity.getGetIdFieldsByUniqueField(field)
                val primaryFieldByUniqField = entity.getGetPrimaryIdFieldByUniqueField(field)
                val fieldGetter = field.getGetterName()
                val primaryIdGetter = entity.getPrimaryField().getGetterName()
                val elsePrefix = if (second) " else " else ""
                code += """${elsePrefix}if (newFullDto.$fieldGetter() != null) {
                if (updateType == EntityUpdateType.UPDATE_RELATION_BY_NAME) {
                    // if we don't have id, have unique field and need update only relation, return only id
                    final Long idByName = $entityRepository.$primaryFieldByUniqField(newFullDto.$fieldGetter());
                    if (idByName != null) {
                        return Pair.of(idByName, null);
                    }
                }

                final $dtoName oldIdFields;
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
                }
            }"""
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

        // final OrderDto orderDtoPre = updater.apply(orderMapper.toOrderDto(orderFullDto));
        val mapperField = entity.getMapperName().lowerFirst()
        val dtoPreField = "${dtoField}Pre"
        statements.add("final $dtoName $dtoPreField = updater.apply($mapperField.to${dtoName}($fullDtoParam));".parseAsStatement())

        // generates save OneToOne and ManyToOne fields
        entity.fields.filter { field -> field.relation == EntityRelation.ManyToOne || field.relation == EntityRelation.OneToOne }
            .forEach { field ->
                val syntheticField = entitiesMap.getSyntheticField(entity, field)
                val fieldGetter = syntheticField.getGetterName()
                val refEntity = entitiesMap.getRefEntity(field)
                val fieldSetter = syntheticField.getSetterName()

                if (field.relation == EntityRelation.OneToOne) {
                    // orderDtoPre.setSelectedItemId(saveOneToOneOrder(orderFullDto.getNextOrder(), oldIdFields != null ? oldIdFields.getNextOrderId() : null));
                    val methodName = "saveOneToOne${refEntity.name}"
                    statements.add(
                        "$dtoPreField.$fieldSetter($methodName($fullDtoParam.${field.getGetterName()}(), oldIdFields != null ? oldIdFields.$fieldGetter() : null));"
                            .parseAsStatement().withLineComment(" save property \"${field.name}\" - OneToOne relation")
                    )
                } else if (field.relation == EntityRelation.ManyToOne) {
                    // orderDtoPre.setSelectedItemId(saveManyToOneItem(orderFullDto.getSelectedItem()));
                    val methodName = "saveManyToOne${refEntity.name}"
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
                val methodName = "save${entity.name}${field.name.upperFirst()}"
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

    private fun addSaveFullEntityMethods(clazz: JavaClassNode, entity: EntityElem): JavaMethodNode {
        /*
        Long saveFullOrder(final OrderFullDto orderFullDto) {
            return saveFullOrder(orderFullDto, UnaryOperator.identity());
        }
        */
        val methodName = "saveFull${entity.name}"
        val fullDtoName = entity.getFullDtoName()
        val fullDtoParam = fullDtoName.lowerFirst()
        clazz.addMethod(methodName)
            .addAnnotation(NotNull::class.java)
            .addParameter(fullDtoName, fullDtoParam)
            .setReturnType(entity.getPrimaryField().asJavaType())
            .setBody("return $methodName($fullDtoParam, UnaryOperator.identity());".parseAsStatement())

        // Long saveFullOrder(final OrderFullDto orderFullDto, final UnaryOperator<OrderDto> updater) {}
        val updaterParam = "updater"
        return clazz.addMethod(methodName)
            .addAnnotation(NotNull::class.java)
            .addParameter(fullDtoName, fullDtoParam)
            .addParameter("UnaryOperator<${entity.getDtoName()}>", updaterParam)
            .setReturnType(entity.getPrimaryField().asJavaType())
    }

    private fun addInnerContextClass(clazz: JavaClassNode, entities: List<EntityElem>): JavaClassNode {
        val contextClass = clazz.addInnerClass("InnerContext").implementInterface(AutoCloseable::class.java)

        entities.forEach { entity ->
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
        return contextClass
    }

    private fun addPublicSaveFullEntityMethods(clazz: JavaClassNode, entity: EntityElem) {
        /*
        public Long saveFullOrder(final OrderFullDto orderFullDto) {
            return saveFullOrder(orderFullDto, EntityUpdateType.DEFAULT);
        }
        */
        val methodName = "saveFull${entity.name}"
        val fullDtoName = entity.getFullDtoName()
        val fullDtoParam = fullDtoName.lowerFirst()
        val entityIdType = entity.getPrimaryFieldType()
        clazz.addMethod(methodName)
            .addAnnotation(NotNull::class.java)
            .withModifiers(Modifier.Keyword.PUBLIC)
            .addParameter(fullDtoName, fullDtoParam)
            .setReturnType(entityIdType)
            .setBody("return saveFull${entity.name}($fullDtoParam, EntityUpdateType.DEFAULT);".parseAsStatement())

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
