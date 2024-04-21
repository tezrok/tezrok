package io.tezrok.logging

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.NameUtil

/**
 * Generate logging related files.
 */
class LoggingFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val moduleName = NameUtil.toHyphenName(module.getName())
        val values = mapOf(
            "moduleName" to moduleName,
            "package" to context.getProject().packagePath
        )
        context.addFile(module.source.main.resources, "/templates/resources/logback.xml.vm", values)

        return true
    }
}
