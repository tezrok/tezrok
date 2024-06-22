package io.tezrok.template

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

/**
 * Adds template related dependencies, templates and classes.
 */
internal class TemplateFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val pom = module.pom
        pom.addDependency("org.apache.velocity:velocity-engine-core:2.3")

        val applicationPackageRoot = module.source.main.java.applicationPackageRoot
        if (applicationPackageRoot != null) {
            context.addFile(
                applicationPackageRoot.getOrAddJavaDirectory("service"),
                "/templates/template/TemplateService.java.vm"
            )
            val values = mapOf("productName" to context.getProject().productName.ifBlank { context.getProject().name })
            val templateDir = module.source.main.resources.addDirectory("templates").addDirectory("velocity")
            context.addFile(templateDir, "/templates/template/velocity/email-send-activation-link.html.vm", values)
            context.addFile(
                templateDir,
                "/templates/template/velocity/email-send-password-recovery-link.html.vm",
                values
            )
        }

        return true
    }
}
