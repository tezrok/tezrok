package io.tezrok.spring

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntityElem
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.getSetterName
import io.tezrok.util.lowerFirst
import org.slf4j.LoggerFactory

/**
 * Creates controller class for each entity.
 */
internal class ControllerFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot
        val pom = module.pom
        pom.addDependency("org.springframework.boot:spring-boot-starter-web:${'$'}{spring-boot.version}")

        if (applicationPackageRoot != null) {
            val projectElem = context.getProject()
            val restDir = applicationPackageRoot.getOrAddJavaDirectory("web").getOrAddJavaDirectory("rest")
            val schema = context.getProject().modules.find { it.name == module.getName() }?.schema

            schema?.entities?.forEach { entity ->
                addControllerClass(entity, restDir, projectElem.packagePath, context)
            }

            addRestHandlerExceptionResolver(applicationPackageRoot.getOrAddJavaDirectory("error"), projectElem.packagePath, context)
            addWebMvcConfig(applicationPackageRoot.getOrAddJavaDirectory("config"), projectElem.packagePath, context)
        } else {
            log.warn("Application package root is not set, module: {}", module.getName())
        }

        return true
    }

    private fun addRestHandlerExceptionResolver(
        restDir: JavaDirectoryNode,
        packagePath: String,
        context: GeneratorContext
    ) {
        val fileName = "RestHandlerExceptionResolver.java"
        if (!restDir.hasClass(fileName)) {
            val controllerFile = restDir.addJavaFile(fileName)
            context.writeTemplate(
                controllerFile,
                "/templates/spring/RestHandlerExceptionResolver.java.vm",
                mapOf("package" to packagePath)
            )
        }
    }

    private fun addWebMvcConfig(
        configDir: JavaDirectoryNode,
        packagePath: String,
        context: GeneratorContext
    ) {
        val fileName = "WebMvcConfig.java"
        if (!configDir.hasClass(fileName)) {
            val controllerFile = configDir.addJavaFile(fileName)
            context.writeTemplate(
                controllerFile,
                "/templates/spring/WebMvcConfig.java.vm",
                mapOf("package" to packagePath)
            )
        }
    }

    private fun addControllerClass(
        entity: EntityElem,
        webDir: JavaDirectoryNode,
        packagePath: String,
        context: GeneratorContext
    ) {
        if (entity.syntheticTo?.isNotBlank() == true) {
            // skip synthetic entities
            return
        }
        if (entity.skipController == true) {
            // skip entities with skipController flag
            return
        }

        val name = entity.name
        val fileName = "${name}ApiController.java"

        if (!webDir.hasClass(fileName)) {
            val controllerFile = webDir.addJavaFile(fileName)
            val values = mapOf(
                "package" to packagePath,
                "name" to name,
                "lname" to name.lowerFirst(),
                "primarySetter" to entity.getPrimaryField().getSetterName()
            )
            context.writeTemplate(controllerFile, "/templates/spring/EntityApiController.java.vm", values)
            // TODO: extract method from service class and add corresponding methods to controller
            // TODO: add custom methods
        } else {
            log.warn("File already exists: {}", fileName)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(ControllerFeature::class.java)!!
    }
}
