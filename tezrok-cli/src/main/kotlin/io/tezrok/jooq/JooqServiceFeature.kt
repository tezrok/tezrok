package io.tezrok.jooq

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
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
                val jooqRepoFile = serviceDir.getOrAddFile("${name}Service.java")
                context.writeTemplate(jooqRepoFile, "/templates/jooq/EntityService.java.vm") { velContext ->
                    velContext.put("package", projectElem.packagePath)
                    velContext.put("name", name)
                }
            }
        } else {
            log.warn("Application package root is not set")
        }

        return true
    }

    private companion object {
        val log = LoggerFactory.getLogger(JooqServiceFeature::class.java)!!
    }
}
