package io.tezrok.web

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

/**
 * Creates BaseApiResult and other API related classes.
 */
internal class WebApiFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val apiDir = applicationPackageRoot.getOrAddJavaDirectory("dto").getOrAddJavaDirectory("api")
            context.addFile(apiDir, "/templates/api/BaseApiResult.java.vm")
        }

        return true
    }
}