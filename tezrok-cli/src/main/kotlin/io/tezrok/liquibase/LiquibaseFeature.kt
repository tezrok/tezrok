package io.tezrok.liquibase

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ModuleNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.api.maven.UseMavenDependency
import io.tezrok.api.sql.SqlGenerator
import io.tezrok.util.PathUtil
import org.apache.velocity.shaded.commons.io.FilenameUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Generates Liquibase changelogs from a JSON schema
 */
@UseMavenDependency("org.liquibase:liquibase-core:${'$'}{liquibase.version}")
internal class LiquibaseFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val sqlGenerator = context.getGenerator(SqlGenerator::class.java)
                ?: throw IllegalArgumentException("SqlGenerator not found")
        val module = project.getSingleModule()
        val schema = context.getProject().modules.find { it.name == module.getName() }?.schema
                ?: throw IllegalArgumentException("No schema found for module ${module.getName()}")
        updateApplicationProperties(module)
        // update pom
        val pomFile = module.pom
        pomFile.addProperty("liquibase.version", "4.22.0")
        // create changelog files
        val resource = module.source.main.resources
        val dbDir = resource.getOrAddDirectory("db")
        val updatesDir = dbDir.getOrAddDirectory("updates")
        val changelogFile = updatesDir.getOrAddFile(datePrefix(context) + "-Initial.sql")
        context.writeTemplate(changelogFile, "/templates/liquibase/changelog.sql.vm") { velContext ->
            velContext.put("author", context.getAuthor())
            velContext.put("name", FilenameUtils.getBaseName(changelogFile.getName()))
            velContext.put("comment", "Initial script for creating the structure")
            velContext.put("sql_statement", sqlGenerator.generate(schema, context).content)
        }

        val masterFile = dbDir.getOrAddFile("master.xml")
        context.writeTemplate(masterFile, "/templates/liquibase/master.xml.vm") { velContext ->
            velContext.put("path", changelogFile.getPathTo(dbDir))
        }

        return true
    }

    private fun updateApplicationProperties(module: ModuleNode) {
        val appProps = module.source.main.resources.getOrAddFile("application.properties")
        val text = appProps.asString()
        // TODO: check properly
        if (!text.contains("spring.liquibase")) {
            val newLines = """
                spring.liquibase.enabled=true
                spring.liquibase.change-log=classpath:db/master.xml${PathUtil.NEW_LINE}
            """.trimIndent()
            appProps.setString(text + newLines)
        }
    }

    private fun datePrefix(context: GeneratorContext): String =
            LocalDateTime.now(context.getClock()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))

    override fun toString(): String {
        return "Feature[LiquibaseGenerator]"
    }
}
