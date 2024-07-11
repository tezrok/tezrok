package io.tezrok.email

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

/**
 * Adds email related dependencies and classes.
 */
internal class EmailFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val pom = module.pom
        pom.addDependency("jakarta.mail:jakarta.mail-api:2.1.3")
        pom.addDependency("org.eclipse.angus:jakarta.mail:2.0.3")

        val applicationPackageRoot = module.source.main.java.applicationPackageRoot
        if (applicationPackageRoot != null) {
            context.addFile(applicationPackageRoot.getOrAddJavaDirectory("service"), "/templates/email/EmailService.java.vm")
        }

        return true
    }
}
