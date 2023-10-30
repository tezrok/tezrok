package io.tezrok.entity

import com.github.javaparser.ast.Modifier
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntityElem
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.maven.ProjectNode
import org.mapstruct.Mapper
import org.slf4j.LoggerFactory

/**
 * Dto mapper related code-generation.
 */
internal class MapperFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val pomFile = module.pom

        pomFile.addProperty("mapstruct.version", "1.5.5.Final")
        pomFile.addDependency("org.mapstruct:mapstruct:${'$'}{mapstruct.version}")

        val pluginNode = pomFile.addPluginDependency("org.apache.maven.plugins:maven-compiler-plugin:3.11.0")
        val configuration = pluginNode.getConfiguration().node
        configuration.getOrAdd("annotationProcessorPaths")
            .getOrAdd("path")
            .getOrAdd("groupId", "org.mapstruct").and()
            .getOrAdd("artifactId", "mapstruct-processor").and()
            .getOrAdd("version", "${'$'}{mapstruct.version}")

        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val schemaModule = context.getProject().modules.find { it.name == module.getName() }
                ?: throw IllegalStateException("Module ${module.getName()} not found")
            val schema = schemaModule.schema
            if (schema != null && schema.entities != null) {
                val projectElem = context.getProject()
                val mapperDir = applicationPackageRoot.getOrAddJavaDirectory("mapper")
                schema.entities.filter { it.isNotSynthetic() }.forEach { entity ->
                    addMapperInterface(mapperDir, entity, projectElem.packagePath)
                }
            }
        } else {
            log.warn("Application package root is not set, module: {}", module.getName())
        }


        return true
    }

    private fun addMapperInterface(mapperDir: JavaDirectoryNode, entity: EntityElem, packagePath: String) {
        val name = entity.name
        val className = "${name}Mapper"
        if (!mapperDir.hasClass(className)) {
            val mapperClass = mapperDir.addClass(className)
                .withModifiers(Modifier.Keyword.PUBLIC)
                .setInterface(true)
                .addAnnotation(Mapper::class.java)

            val dtoType = "${name}Dto"
            val fullDtoType = "${name}FullDto"
            mapperClass.addMethod("to$dtoType")
                .setReturnType(dtoType)
                .addParameter(fullDtoType, "fullDto")
                .removeBody()
            mapperClass.addMethod("to$fullDtoType")
                .setReturnType(fullDtoType)
                .addParameter(dtoType, "dto")
                .removeBody()

            mapperClass.addImport("$packagePath.dto.$dtoType");
            mapperClass.addImport("$packagePath.dto.full.$fullDtoType");
        } else {
            log.warn("Mapper class {} already exists", className)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(MapperFeature::class.java)!!
    }
}
