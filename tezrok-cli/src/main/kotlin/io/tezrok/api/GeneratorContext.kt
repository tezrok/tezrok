package io.tezrok.api

import io.tezrok.api.input.ProjectElem
import io.tezrok.api.io.OutStream
import io.tezrok.api.node.DirectoryNode
import io.tezrok.api.node.StoreStrategy
import io.tezrok.util.ResourceUtil
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
        writeTemplate(output, templatePath) { context ->
            extendsValues(
                templatePath,
                values
            ).forEach { (k, v) -> context.put(k, v) }
        }

    /**
     * Writes content to [OutStream] using specified template.
     */
    fun writeTemplate(output: OutStream, templatePath: String) =
        writeTemplate(output, templatePath, extendsValues(templatePath))

    /**
     * Add a file to the target directory with the specified template.
     * Name of the file is the same as the template name without the .vm extension.
     */
    fun addFile(
        targetDir: DirectoryNode,
        templatePath: String,
        values: Map<String, Any?> = emptyMap(),
        strategy: StoreStrategy = StoreStrategy.SAVE
    ) {
        val templateName = FilenameUtils.getName(templatePath)
        if (templateName.endsWith(".vm")) {
            val fileName = templateName.removeSuffix(".vm")
            val startDbFile = targetDir.getOrAddFile(fileName)
            if (startDbFile.isEmpty()) {
                startDbFile.strategy = strategy
                writeTemplate(startDbFile, templatePath, extendsValues(templatePath, values))
            } else {
                log.warn("File {} already exists, skipping", fileName)
            }
        } else {
            check(values.isEmpty()) { "Values are not supported for non-vm files" }
            val startDbFile = targetDir.getOrAddFile(templateName)
            if (startDbFile.isEmpty()) {
                startDbFile.strategy = strategy
                startDbFile.getOutputStream().use { os ->
                    ResourceUtil.getResourceAsStream(templatePath).use { inputStream ->
                        inputStream.copyTo(os)
                    }
                }
            } else {
                log.warn("File {} already exists, skipping", templateName)
            }
        }
    }

    fun extendsValues(templatePath: String, values: Map<String, Any?> = emptyMap()): Map<String, Any?> {
        return if (values.containsKey("package") || !templatePath.endsWith(".java.vm"))
            values
        else
            values + mapOf("package" to getProject().packagePath)
    }

    companion object {
        private val log = LoggerFactory.getLogger(GeneratorContext::class.java)!!
    }
}
