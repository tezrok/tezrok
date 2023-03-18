package io.tezrok.util

import org.apache.velocity.Template
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader

/**
 * Velocity utilities
 */
object VelocityUtil {
    /**
     * Returns a Velocity template from the resources
     */
    fun getTemplate(filePath: String): Template {
        val engine = VelocityEngine()
        engine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath")
        engine.setProperty("resource.loader.classpath.class", ClasspathResourceLoader::class.java.name)
        return engine.getTemplate(filePath, "UTF-8")
    }
}
