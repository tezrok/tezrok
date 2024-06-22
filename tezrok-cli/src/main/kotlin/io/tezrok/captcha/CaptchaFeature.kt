package io.tezrok.captcha

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

/**
 * Adds captcha related dependencies and classes.
 */
internal class CaptchaFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot
        if (applicationPackageRoot != null) {
            val serviceDir = applicationPackageRoot.getOrAddJavaDirectory("service")
            context.addFile(
                serviceDir, "/templates/captcha/CaptchaService.java.vm"
            )
        }

        return true
    }
}
