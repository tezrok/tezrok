package io.tezrok.jooq

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.maven.ProjectNode
import org.slf4j.LoggerFactory

/**
 * Creates service class for each entity.
 */
open class JooqServiceFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val projectElem = context.getProject()
            val serviceDir = applicationPackageRoot.getOrAddJavaDirectory("service")
            val schemaModule = context.getProject().modules.find { it.name == module.getName() }
                    ?: throw IllegalStateException("Module ${module.getName()} not found")

            schemaModule.schema?.definitions?.keys?.forEach { name ->
                addServiceClass(name, serviceDir, projectElem.packagePath, context)
            }
        } else {
            log.warn("Application package root is not set")
        }

        return true
    }

    private fun addServiceClass(name: String, serviceDir: JavaDirectoryNode, packagePath: String, context: GeneratorContext) {
        val fileName = "${name}Service.java"
        if (!serviceDir.hasFile(fileName)) {
            val jooqRepoFile = serviceDir.addJavaFile(fileName)
            val values = mapOf("package" to packagePath, "name" to name)
            context.writeTemplate(jooqRepoFile, "/templates/jooq/EntityService.java.vm", values)
        } else {
            log.warn("File already exists: {}", fileName)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(JooqServiceFeature::class.java)!!
    }
}
