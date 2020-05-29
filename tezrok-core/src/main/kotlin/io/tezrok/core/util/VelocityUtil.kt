package io.tezrok.core.util

import org.apache.velocity.Template
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader

object VelocityUtil {
    fun getTemplate(filePath: String): Template {
        val engine = VelocityEngine()
        engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
        engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader::class.java.name)

        return engine.getTemplate(filePath, "UTF-8")
    }
}
