package io.tezrok.jooq

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.ThrowStmt
import com.github.javaparser.ast.type.ClassOrInterfaceType
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntityElem
import io.tezrok.api.java.*
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.*
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

        // add getStub method
        val entitiesMap = entities.associateBy { it.name }
        addGetStubMethod(clazz, entitiesMap)

        // implement public load methods
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
        val repositoryFieldName = entity.getRepositoryName().lowerFirst()

        // code: EntityDto entityDto = entityRepository.getById(id);
        statements.add(
            JavaCallExpressionNode.ofMethodCall("$repositoryFieldName.getById")
                .addNameArgument("id")
                .assignToAsStatement(dtoField)
                .apply { setLineComment("get root dto") }
        )

        // code: EntityFullDto entityFullDto = entityMapper.toEntityFullDto(entityDto);
        val mapperFieldName = entity.getMapperName().lowerFirst()
        statements.add(
            JavaCallExpressionNode.ofMethodCall("$mapperFieldName.to$fullDtoName")
                .addNameArgument(dtoFieldName)
                .assignToAsStatement(fullDtoField)
                .apply { setLineComment("convert to full dto") }
        )

        // code: IdTracer tracer = new IdTracer(itemFullDto, this::getStub);
        val tracerName = "IdTracer"
        val tracerFieldName = "tracer"
        val tracerType = ClassOrInterfaceType(tracerName)
        val tracerField = VariableDeclarationExpr(tracerType, tracerFieldName, Modifier.finalModifier())
        val thisGetStubRef = NameExpr("this::getStub")
        statements.add(
            ObjectCreationExpr(
                null, tracerType,
                NodeList(dtoFullFieldName.asNameExpr(), thisGetStubRef)
            ).assignToAsStatement(tracerField)
        )

        // code: return itemFullDto
        statements.add(ReturnStmt(dtoFullFieldName))

        // TODO: add the rest of the code
        method.setBody(statements)
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

    private fun addGetStubMethod(clazz: JavaClassNode, entitiesMap: Map<String, EntityElem>) {
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
        entitiesMap.values.filter { it.isNotSynthetic() }
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
            ))

        method.setBody(statements)
    }


    private companion object {
        val log = LoggerFactory.getLogger(EntityGraphLoaderFeature::class.java)!!
    }
}
