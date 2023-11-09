package io.tezrok.jooq

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.ThrowStmt
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntityElem
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.java.JavaFieldNode
import io.tezrok.api.java.JavaMethodNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.asJavaType
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
            addFields(clazz, entity, fields)
        }
        fields.forEach(clazz::initInConstructor)

        // add public load methods
        val methods = mutableListOf<JavaMethodNode>()
        entities.filter { it.isNotSynthetic() }.forEach { entity ->
            methods.add(addPublicLoadMethod(clazz, entity, packagePath))
        }
    }

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
            .addParameter(primaryField.asJavaType(), "id")
            .setReturnType(fullDtoName)
            .setBody(ThrowStmt(NameExpr("new UnsupportedOperationException()")))

        clazz.addImport("$packagePath.dto.full.$fullDtoName")

        return method
    }

    private fun addFields(
        clazz: JavaClassNode,
        entity: EntityElem,
        fields: MutableList<JavaFieldNode>
    ) {
        val name = entity.name
        val repositoryName = "${name}Repository"

        val repoField = clazz.addField(repositoryName, repositoryName.decapitalize())
            .withModifiers(Modifier.Keyword.FINAL)
        fields.add(repoField)
    }

    private fun addIdTracer(repositoryDir: JavaDirectoryNode, packagePath: String, context: GeneratorContext) {
        val values = mapOf("package" to packagePath)
        val idTracerFile = repositoryDir.addJavaFile("IdTracer.java")
        context.writeTemplate(idTracerFile, "/templates/jooq/IdTracer.java.vm", values)
    }

    private companion object {
        val log = LoggerFactory.getLogger(EntityGraphLoaderFeature::class.java)!!
    }
}
