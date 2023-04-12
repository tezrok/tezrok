package io.tezrok.liquibase

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.node.FileNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.api.sql.SqlGenerator
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
class LiquibaseGenerator : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext) {
        val sqlGenerator = context.getGenerator(SqlGenerator::class.java)
            ?: throw IllegalArgumentException("SqlGenerator not found")
        // TODO: add maven dependency
        check(project.getModules().size == 1) { "Liquibase feature only supports one module" }
        // TODO: support multiple modules
        val module = project.getModules()[0]
        val schema = context.getProject().modules.find { it.name == module.getName() }?.schema
            ?: throw IllegalArgumentException("No schema found for module ${module.getName()}")
        val resource = module.getResources()
        val dbDir = resource.getOrAddDirectory("db")
        val updatesDir = dbDir.getOrAddDirectory("updates")
        val changelogFile = updatesDir.getOrAddFile(datePrefix(context) + "-Initial.sql")
        writeFile(changelogFile, context, "/templates/liquibase/changelog.sql.vm") { velContext ->
            velContext.put("author", context.getAuthor())
            velContext.put("name", FilenameUtils.getBaseName(changelogFile.getName()))
            velContext.put("comment", "Initial script for creating the structure")
            velContext.put("sql_statement", sqlGenerator.generate(schema, context).content)
        }

        val masterFile = dbDir.getOrAddFile("master.xml")
        writeFile(masterFile, context, "/templates/liquibase/master.xml.vm") { velContext ->
            velContext.put("path", changelogFile.getPathTo(dbDir))
        }
    }

    private fun datePrefix(context: GeneratorContext): String =
        LocalDateTime.now(context.getClock()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))

    private fun writeFile(
        file: FileNode,
        context: GeneratorContext,
        templatePath: String,
        contextInitializer: Consumer<VelocityContext>
    ) {
        val masterTemplate = VelocityUtil.getTemplate(templatePath)
        val velocityContext = VelocityContext()

        if (context.isGenerateTime()) {
            velocityContext.put(
                "generateTime",
                LocalDateTime.now(context.getClock()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
        contextInitializer.accept(velocityContext)

        OutputStreamWriter(file.getOutputStream(), context.getCharset()).use { writer ->
            masterTemplate.merge(velocityContext, writer)
        }
    }
}
