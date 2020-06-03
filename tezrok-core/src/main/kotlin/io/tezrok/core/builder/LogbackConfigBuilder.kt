package io.tezrok.core.builder

import io.tezrok.api.ExecuteContext
import io.tezrok.api.builder.VelocityBuilder
import io.tezrok.core.generator.toMavenVersion
import io.tezrok.core.util.VelocityUtil
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext

/**
 * Generates logback.xml
 */
class LogbackConfigBuilder(context: ExecuteContext) : VelocityBuilder(context) {
    override fun onBuild(velContext: VelocityContext) {
        // TODO: use unified method of space-less project name
        velContext.put("filename", context.module.toMavenVersion().artifactId)
        velContext.put("packagePath", context.module.packagePath)
    }

    override fun getFileName(): String = "logback.xml"

    override fun getPath(): String = "src/main/resources"

    override fun getTemplate(): Template = TEMPLATE

    companion object {
        private val TEMPLATE: Template = VelocityUtil.getTemplate("templates/logback.xml.vm")
    }
}
