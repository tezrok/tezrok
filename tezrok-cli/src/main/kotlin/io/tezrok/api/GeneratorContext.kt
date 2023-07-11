package io.tezrok.api

import io.tezrok.api.io.OutStream
import io.tezrok.api.input.ProjectElem
import org.apache.velocity.VelocityContext
import java.io.Writer
import java.time.Clock
import java.util.function.Consumer

/**
 * Used to provide context for any [TezrokGenerator] or [TezrokFeature]
 */
interface GeneratorContext : GeneratorProvider {
    fun isGenerateTime(): Boolean = true

    fun getAuthor(): String

    fun getClock(): Clock = Clock.systemDefaultZone()

    fun getProject(): ProjectElem

    /**
     * Writes content to [Writer] using specified template.
     */
    fun writeTemplate(writer: Writer, templatePath: String, contextInitializer: Consumer<VelocityContext>)

    /**
     * Writes content to [Writer] using specified template.
     */
    fun writeTemplate(writer: Writer, templatePath: String, values: Map<String, Any?>) =
            writeTemplate(writer, templatePath) { context -> values.forEach { (k, v) -> context.put(k, v) } }

    /**
     * Writes content to [OutStream] using specified template.
     */
    fun writeTemplate(output: OutStream, templatePath: String, contextInitializer: Consumer<VelocityContext>) =
            output.getOutputStream().bufferedWriter().use { writeTemplate(it, templatePath, contextInitializer) }

    /**
     * Writes content to [OutStream] using specified template.
     */
    fun writeTemplate(output: OutStream, templatePath: String, values: Map<String, Any?>) =
            writeTemplate(output, templatePath) { context -> values.forEach { (k, v) -> context.put(k, v) } }
}
