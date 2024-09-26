package io.tezrok.util

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

internal class UtilsFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val utilDir = applicationPackageRoot.getOrAddJavaDirectory("util")
            context.addFile(utilDir, "/templates/util/TimeUtil.java.vm")
            context.addFile(utilDir, "/templates/util/WebUtil.java.vm")
        }

        return true
    }
}
