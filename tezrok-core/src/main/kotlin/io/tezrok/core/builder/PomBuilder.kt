package io.tezrok.core.builder

import io.tezrok.api.GeneratorContext
import io.tezrok.api.model.maven.Pom
import io.tezrok.api.util.VelocityUtil
import org.apache.commons.lang3.StringUtils
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext

class PomBuilder(private val pom: Pom, context: GeneratorContext) : BaseBuilder(context) {
    override fun onBuild(velContext: VelocityContext) {
        velContext.put("version", pom.version)
        velContext.put("type", pom.type)
        velContext.put("properties", pom.properties)
        velContext.put("dependencies", pom.dependencies)
    }

    override fun getPath(): String = StringUtils.EMPTY

    override fun getFileName(): String = "pom.xml"

    override fun getTemplate(): Template = template

    companion object {
        private val template: Template = VelocityUtil.getTemplate("templates/pom.xml.vm")
    }
}
