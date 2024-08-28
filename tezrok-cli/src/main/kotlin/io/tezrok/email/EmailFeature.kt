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
            val moduleElem = context.getProject().getModule(module.getName())
            val serviceDir = applicationPackageRoot.getOrAddJavaDirectory("service")
            val values = mapOf("newEntityRecordsFeature" to (moduleElem.newRecords == true))
            context.addFile(serviceDir, "/templates/email/EmailService.java.vm", values)
        }

        return true
    }
}
