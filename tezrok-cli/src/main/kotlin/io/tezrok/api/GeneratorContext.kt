package io.tezrok.api

import io.tezrok.api.input.ProjectElem
import io.tezrok.api.io.OutStream
import io.tezrok.api.node.DirectoryNode
import org.apache.velocity.VelocityContext
import org.apache.velocity.shaded.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import java.io.Writer
import java.time.Clock
import java.util.function.Consumer

/**
 * Used to provide context for any [TezrokGenerator] or [TezrokFeature]
 */
interface GeneratorContext : GeneratorProvider {
    fun isGenerateTime(): Boolean = true

    fun getAuthorLogin(): String

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

    /**
     * Writes content to [OutStream] using specified template.
     */
    fun writeTemplate(output: OutStream, templatePath: String) =
        writeTemplate(output, templatePath, mapOf("package" to getProject().packagePath))

    /**
     * Add a file to the target directory with the specified template.
     * Name of the file is the same as the template name without the .vm extension.
     */
    fun addFile(targetDir: DirectoryNode, templatePath: String, values: Map<String, String?> = emptyMap()) {
        val fileName = FilenameUtils.getName(templatePath).removeSuffix(".vm")
        val startDbFile = targetDir.getOrAddFile(fileName)
        if (startDbFile.isEmpty()) {
            if (fileName.endsWith(".java")) {
                val finalValues = if (values.containsKey("package"))
                    values
                else
                    values + mapOf("package" to getProject().packagePath)
                writeTemplate(startDbFile, templatePath, finalValues)
            } else {
                writeTemplate(startDbFile, templatePath, values)
            }
        } else {
            log.warn("File {} already exists, skipping", fileName)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GeneratorContext::class.java)!!
    }
}
