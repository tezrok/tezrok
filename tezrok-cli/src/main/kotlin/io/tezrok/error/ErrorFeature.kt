package io.tezrok.error

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.hyphenNameToCamelCase
import io.tezrok.util.upperFirst

/**
 * Generate base exception and other error classes.
 */
internal class ErrorFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val moduleName = module.getName().hyphenNameToCamelCase().upperFirst()
            val values = mapOf(
                "moduleName" to moduleName,
            )
            val errorDir = applicationPackageRoot.getOrAddJavaDirectory("error")
            val baseExceptionFile = errorDir.addJavaFile("Base${moduleName}Exception")
            context.writeTemplate(baseExceptionFile, "/templates/error/BaseModuleException.java.vm", values)
            context.addFile(errorDir, "/templates/error/ForbiddenAccessException.java.vm", values)
            context.addFile(errorDir, "/templates/error/UserNotFoundException.java.vm", values)
            context.addFile(errorDir, "/templates/error/UserAlreadyExistsException.java.vm", values)
        }

        return true
    }
}
