package io.tezrok.liquibase

import io.tezrok.core.GeneratorContext
import io.tezrok.core.common.FileNode
import io.tezrok.core.output.ModuleNode
import io.tezrok.schema.Schema
import io.tezrok.sql.SqlGenerator
import io.tezrok.util.VelocityUtil
import org.apache.velocity.VelocityContext
import org.apache.velocity.shaded.commons.io.FilenameUtils
import java.io.OutputStreamWriter
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer


/**
 * Generates Liquibase changelogs from a JSON schema
 */
class LiquibaseGenerator(
    private val genContext: GeneratorContext,
    private val sqlGenerator: SqlGenerator,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun generate(schema: Schema, module: ModuleNode) {
        val resource = module.getResources()
        val dbDir = resource.getOrAddDirectory("db")
        val updatesDir = dbDir.getOrAddDirectory("updates")
        val changelogFile = updatesDir.getOrAddFile(datePrefix() + "-Initial.sql")
        writeFile(changelogFile, "/templates/liquibase/changelog.sql.vm") { context ->
            context.put("author", genContext.getAuthor())
            context.put("name", FilenameUtils.getBaseName(changelogFile.getName()))
            context.put("comment", "Initial script for creating the structure")
            context.put("sql_statement", sqlGenerator.generateAsString(schema))
        }

        val masterFile = dbDir.getOrAddFile("master.xml")
        writeFile(masterFile, "/templates/liquibase/master.xml.vm") { context ->
            context.put("path", changelogFile.getPathTo(dbDir))
        }
    }

    private fun datePrefix(): String = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))

    private fun writeFile(file: FileNode, templatePath: String, contextInitializer: Consumer<VelocityContext>) {
        val masterTemplate = VelocityUtil.getTemplate(templatePath)
        val context = VelocityContext()

        if (genContext.isGenerateTime()) {
            context.put("generateTime", LocalDateTime.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        }
        contextInitializer.accept(context)

        OutputStreamWriter(file.getOutputStream(), genContext.getCharset()).use { writer ->
            masterTemplate.merge(context, writer)
        }
    }
}
