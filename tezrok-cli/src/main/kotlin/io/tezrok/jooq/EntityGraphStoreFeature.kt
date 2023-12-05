package io.tezrok.jooq

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.stmt.Statement
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntityElem
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.java.JavaFieldNode
import io.tezrok.api.java.JavaMethodNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.*
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
        methods.forEach { (entity, method) ->
            implementSaveFullEntityMethod(method, entity)
        }
    }

    private fun implementSaveFullEntityMethod(method: JavaMethodNode, entity: EntityElem) {
        val statements = NodeList<Statement>()

        // if (orderFullDto == null) {return null;}
        val fullDtoName = entity.getFullDtoName()
        val fullDtoParam = fullDtoName.lowerFirst()
        statements.add("if ($fullDtoParam == null) {return null;}".parseAsStatement())

        val entityName = entity.name
        val dtoName = entity.getDtoName()
        val dtoParam = dtoName.lowerFirst()
        val idType = entity.getPrimaryField().asJavaType()
        // Pair<Long, OrderDto> returnOrIds = loadOrderIdFieldsOrReturn(orderFullDto);
        val loadMethod = entity.getLoadEntityIdFieldsOrReturnMethod()
        statements.add(
            "final Pair<$idType, $dtoName> returnOrIds = $loadMethod($fullDtoParam);".parseAsStatement()
                .withLineComment("old $entityName ids fields")
        )

        // if (returnOrIds.getLeft() != null) { return returnOrIds.getLeft(); }
        statements.add("if (returnOrIds.getLeft() != null) { return returnOrIds.getLeft(); }".parseAsStatement())
        // final OrderDto oldIdFields = returnOrIds.getRight();
        statements.add("final $dtoName oldIdFields = returnOrIds.getRight();".parseAsStatement())

        // generates save OneToOne and ManyToOne fields
        val idFields = entity.getIdFields().filter { field -> field.external != true && field.primary != true }
        if (idFields.isNotEmpty()) {
            idFields.forEach { field ->
                // TODO: add support for ManyToOne and OneToOne fields
            }
        }

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
            .addParameter(fullDtoName, fullDtoParam)
            .setBody("return $methodName($fullDtoParam, UnaryOperator.identity());".parseAsStatement())

        // Long saveFullOrder(final OrderFullDto orderFullDto, final UnaryOperator<OrderDto> updater) {}
        val updaterParam = "updater"
        return clazz.addMethod(methodName)
            .addParameter(fullDtoName, fullDtoParam)
            .addParameter("UnaryOperator<$fullDtoName>", updaterParam)
            .setReturnType(entity.getPrimaryField().asJavaType())
    }

    private fun addInnerContextClass(clazz: JavaClassNode, entities: List<EntityElem>): JavaClassNode {
        val contextClass = clazz.addInnerClass("InnerContext")

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
        public void saveFullOrder(final OrderFullDto orderFullDto) {
            saveFullOrder(orderFullDto, EntityUpdateType.DEFAULT);
        }
        */
        val methodName = "saveFull${entity.name}"
        val fullDtoName = entity.getFullDtoName()
        val fullDtoParam = fullDtoName.lowerFirst()
        clazz.addMethod(methodName)
            .withModifiers(Modifier.Keyword.PUBLIC)
            .addParameter(fullDtoName, fullDtoParam)
            .setBody("saveFull${entity.name}($fullDtoParam, EntityUpdateType.DEFAULT);".parseAsStatement())

        /*
        public void saveFullOrder(final OrderFullDto orderFullDto, EntityUpdateType updateType) {
            final InnerContext ctx = new InnerContext(updateType);
            ctx.saveFullOrder(orderFullDto);
            ctx.close();
        }
        */
        val updateTypeParam = "updateType"
        clazz.addMethod(methodName)
            .withModifiers(Modifier.Keyword.PUBLIC)
            .addParameter(fullDtoName, fullDtoParam)
            .addParameter("EntityUpdateType", updateTypeParam)
            .setBody(
                """final InnerContext ctx = new InnerContext($updateTypeParam);
            ctx.$methodName($fullDtoParam);
            ctx.close();""".parseAsBlock()
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
