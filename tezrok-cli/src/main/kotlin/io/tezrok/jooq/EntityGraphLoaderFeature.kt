package io.tezrok.jooq

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.ClassOrInterfaceType
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntityElem
import io.tezrok.api.java.*
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.asJavaType
import io.tezrok.util.lowerFirst
import org.jetbrains.annotations.Nullable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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
                addEntityGraphLoader(repositoryDir, entities, projectElem.packagePath)
            }
        } else {
            log.warn("Application package root is not set")
        }

        return true
    }

    private fun addEntityGraphLoader(
        repositoryDir: JavaDirectoryNode,
        entities: List<EntityElem>,
        packagePath: String
    ) {
        val clazz = repositoryDir.addClass("EntityGraphLoader")
            .addAnnotation(Service::class.java)
            .setModifiers(Modifier.Keyword.PUBLIC)
        // add fields which are initialized in constructor
        val fields = mutableListOf<JavaFieldNode>()
        entities.forEach { entity ->
            addFields(clazz, entity, fields, packagePath)
        }
        fields.forEach(clazz::initInConstructor)

        // add public load methods
        val methods = mutableMapOf<EntityElem, JavaMethodNode>()
        entities.filter { it.isNotSynthetic() }.forEach { entity ->
            methods[entity] = addPublicLoadMethod(clazz, entity, packagePath)
        }

        // implement public load methods
        val entitiesMap = entities.associateBy { it.name }
        methods.forEach { (entity, method) ->
            implementPublicLoadMethod(method, entity, entitiesMap)
        }
    }

    /**
     * Adds public method to load entity by id.
     */
    private fun addPublicLoadMethod(
        clazz: JavaClassNode,
        entity: EntityElem,
        packagePath: String
    ): JavaMethodNode {
        val name = entity.name
        check(entity.getPrimaryFieldCount() == 1) { "Entity $name expected have exactly one primary field" }
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

        clazz.addImport("$packagePath.dto.${name}Dto")
        clazz.addImport("$packagePath.dto.full.$fullDtoName")

        return method
    }

    private fun implementPublicLoadMethod(
        method: JavaMethodNode,
        entity: EntityElem,
        entitiesMap: Map<String, EntityElem>
    ) {
        val statements = NodeList<Statement>()
        val dtoName = entity.getDtoName()
        val dtoFieldName = entity.getDtoName().lowerFirst()
        val dtoField = VariableDeclarationExpr(ClassOrInterfaceType(dtoName), dtoFieldName, Modifier.finalModifier())
        val fullDtoName = entity.getFullDtoName()
        val dtoFullFieldName = fullDtoName.lowerFirst()
        val fullDtoField =
            VariableDeclarationExpr(ClassOrInterfaceType(fullDtoName), dtoFullFieldName, Modifier.finalModifier())
        val repositoryName = entity.getRepositoryName()
        val repositoryFieldName = entity.getRepositoryName().lowerFirst()
        val mapperName = entity.getMapperName()
        val mapperFieldName = entity.getMapperName().lowerFirst()

        statements.add(
            JavaCallExpressionNode.ofMethodCall("$repositoryFieldName.getById")
                .addNameArgument("id")
                .assignToAsStatement(dtoField)
        )

        statements.add(
            JavaCallExpressionNode.ofMethodCall("$mapperFieldName.to$fullDtoName")
                .addNameArgument(dtoFieldName)
                .assignToAsStatement(fullDtoField)
        )

        statements.add(ReturnStmt(dtoFullFieldName))
        method.setBody(BlockStmt(statements))
    }

    private fun addFields(
        clazz: JavaClassNode,
        entity: EntityElem,
        fields: MutableList<JavaFieldNode>,
        packagePath: String
    ) {
        val repositoryName = entity.getRepositoryName()

        val repoField = clazz.addField(repositoryName, repositoryName.lowerFirst())
            .withModifiers(Modifier.Keyword.FINAL)
        fields.add(repoField)

        if (entity.isNotSynthetic()) {
            val mapperName = entity.getMapperName()
            val mapperField = clazz.addField(mapperName, mapperName.lowerFirst())
                .withModifiers(Modifier.Keyword.FINAL)
            fields.add(mapperField)
            clazz.addImport("$packagePath.mapper.$mapperName")
        }
    }

    private fun addIdTracer(repositoryDir: JavaDirectoryNode, packagePath: String, context: GeneratorContext) {
        val values = mapOf("package" to packagePath)
        val idTracerFile = repositoryDir.addJavaFile("IdTracer.java")
        context.writeTemplate(idTracerFile, "/templates/jooq/IdTracer.java.vm", values)
    }

    private fun EntityElem.getRepositoryName(): String = "${name}Repository"


    private fun EntityElem.getMapperName(): String = "${name}Mapper"

    private fun EntityElem.getDtoName(): String = "${name}Dto"

    private fun EntityElem.getFullDtoName(): String = "${name}FullDto"

    private companion object {
        val log = LoggerFactory.getLogger(EntityGraphLoaderFeature::class.java)!!
    }
}
