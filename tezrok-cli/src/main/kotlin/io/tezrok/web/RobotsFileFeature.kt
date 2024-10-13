package io.tezrok.web

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

/**
 * Creates robots.txt file and related controller which serves it depending on the active profile.
 */
internal class RobotsFileFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val appPackageRoot = module.source.main.java.applicationPackageRoot
        if (appPackageRoot != null) {
            val moduleElem = context.getProject().getModule(module.getName())
            if (moduleElem.hasRobotsFile()) {
                val webDir = appPackageRoot.getOrAddJavaDirectory("web")
                context.addFile(webDir, "/templates/web/RobotsFileController.java.vm")
                val thymeleafDir = module.source.main.resources.getOrAddDirectory("templates/thymeleaf")
                context.addFile(thymeleafDir, "/templates/web/robots-dev.txt")
                context.addFile(thymeleafDir, "/templates/web/robots-prod.txt")
            }
        }
        return true
    }
}
