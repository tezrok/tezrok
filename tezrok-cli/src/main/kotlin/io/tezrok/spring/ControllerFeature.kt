package io.tezrok.spring

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.maven.ProjectNode
import org.slf4j.LoggerFactory

/**
 * Creates controller class for each entity.
 */
internal class ControllerFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot
        val pom = module.pom
        pom.addDependency("org.springframework.boot:spring-boot-starter-web")

        if (applicationPackageRoot != null) {
            val projectElem = context.getProject()
            val webDir = applicationPackageRoot.getOrAddJavaDirectory("web").getOrAddJavaDirectory("rest")
            val schema = context.getProject().modules.find { it.name == module.getName() }?.schema

            schema?.entities?.forEach { entity ->
                addControllerClass(entity.name, webDir, projectElem.packagePath, context)
            }
        } else {
            log.warn("Application package root is not set, module: {}", module.getName())
        }

        return true
    }

    private fun addControllerClass(name: String, webDir: JavaDirectoryNode, packagePath: String, context: GeneratorContext) {
        val fileName = "${name}Controller.java"

        if (!webDir.hasClass(fileName)) {
            val controllerFile = webDir.addJavaFile(fileName)
            val values = mapOf("package" to packagePath, "name" to name, "lname" to name.replaceFirstChar { it.lowercase() })
            context.writeTemplate(controllerFile, "/templates/spring/EntityController.java.vm", values)
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
