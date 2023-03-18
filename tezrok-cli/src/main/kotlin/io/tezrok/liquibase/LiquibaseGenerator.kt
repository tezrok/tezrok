package io.tezrok.liquibase

import io.tezrok.core.GeneratorContext
import io.tezrok.core.common.FileNode
import io.tezrok.core.output.ModuleNode
import io.tezrok.schema.Schema
import io.tezrok.util.VelocityUtil
import org.apache.velocity.VelocityContext
import org.apache.velocity.shaded.commons.io.FilenameUtils
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer


/**
 * Generates Liquibase changelogs from a JSON schema
 */
class LiquibaseGenerator(private val genContext: GeneratorContext) {
    fun generate(schema: Schema, module: ModuleNode) {
        val resource = module.getResources()
        val dbDir = resource.getOrAddDirectory("db")
        val updatesDir = dbDir.getOrAddDirectory("updates")
        val changelogFile = updatesDir.getOrAddFile("changelog.sql")
        writeFile(changelogFile, "/templates/liquibase/changelog.sql.vm") { context ->
            context.put("author", genContext.getAuthor())
            context.put("name", FilenameUtils.getBaseName(changelogFile.getName()))
            context.put("comment", "Initial script for creating the structure")
            context.put("sql_statement", generateSql(schema))
        }

        val masterFile = dbDir.getOrAddFile("master.xml")
        writeFile(masterFile, "/templates/liquibase/master.xml.vm") { context ->
            context.put("path", schema)
        }
    }

    private fun generateSql(schema: Schema): String {
        // TODO: Generate real SQL from schema
        return """CREATE TABLE XXXX (
                id SERIAL PRIMARY KEY,
                street VARCHAR(255) NOT NULL,
                city VARCHAR(100) NOT NULL,
                state VARCHAR(100) NOT NULL,
                zip VARCHAR(10) NULL,
                country VARCHAR(100) NOT NULL
            );
        """.trimIndent()
    }

    private fun writeFile(file: FileNode, templatePath: String, contextInitializer: Consumer<VelocityContext>) {
        val masterTemplate = VelocityUtil.getTemplate(templatePath)
        val context = VelocityContext()

        if (genContext.isGenerateTime()) {
            context.put("generateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        }
        contextInitializer.accept(context)

        file.getOutputStream().use { stream ->
            masterTemplate.merge(context, OutputStreamWriter(stream, genContext.getCharset()))
        }
    }
}
