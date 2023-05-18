package io.tezrok.liquibase

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.BuildPhase
import io.tezrok.api.maven.PomNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.api.maven.UseMavenDependency
import io.tezrok.api.node.FileNode
import io.tezrok.api.sql.SqlGenerator
import io.tezrok.util.VelocityUtil
import org.apache.velocity.VelocityContext
import org.apache.velocity.shaded.commons.io.FilenameUtils
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer


/**
 * Generates Liquibase changelogs from a JSON schema
 */
@UseMavenDependency("org.liquibase:liquibase-core:3.8.9")
class LiquibaseGenerator : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val sqlGenerator = context.getGenerator(SqlGenerator::class.java)
            ?: throw IllegalArgumentException("SqlGenerator not found")
        check(project.getModules().size == 1) { "Liquibase feature only supports one module" }
        // TODO: support multiple modules
        val projectElem = context.getProject()
        val module = project.getModules()[0]
        val schema = context.getProject().modules.find { it.name == module.getName() }?.schema
            ?: throw IllegalArgumentException("No schema found for module ${module.getName()}")
        val resource = module.source.main.resources
        // update pom
        val pomFile = module.pom
        pomFile.dependencyId = pomFile.dependencyId
            .withGroupId(projectElem.packagePath)
            .withVersion(projectElem.version)
        pomFile.addDependency("org.postgresql:postgresql:42.6.0")
        pomFile.addDependency("org.jooq:jooq:${'$'}{jooq.version}")
        pomFile.addProperty("testcontainers.version", "1.18.0")
        pomFile.addProperty("liquibase.version", "3.8.9")
        pomFile.addProperty("jooq.version", "3.13.4")
        pomFile.addProperty("db.username", "postgres")
        pomFile.addProperty("db.password", "postgres")
        addGroovyPlugin(pomFile)
        addLiquibasePlugin(pomFile)
        addJooqPlugin(pomFile, projectElem.packagePath + ".jooq")

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

        return true
    }

    private fun addGroovyPlugin(pomFile: PomNode) {
        val pluginNode = pomFile.addPluginDependency("org.codehaus.gmaven:groovy-maven-plugin:2.1.1")
        val executionStart = pluginNode.addExecution("testcontainer-start", BuildPhase.GenerateSources, "execute")
        val configurationStart = executionStart.getConfiguration()
        configurationStart.node.add(
            "source",
            """
                db = new org.testcontainers.containers.PostgreSQLContainer("postgres:$POSTGRESQL_VER")
                    .withUsername("${'$'}{db.username}")
                    .withDatabaseName("postgres")
                    .withPassword("${'$'}{db.password}");
                                        
                db.start();
                project.properties.setProperty('db.url', db.getJdbcUrl());
                project.properties.setProperty('testcontainer.containerid', db.getContainerId());
                project.properties.setProperty('testcontainer.imageName', db.getDockerImageName());
            """
        )

        val executionStop = pluginNode.addExecution("testcontainer-stop", BuildPhase.Test, "execute")
        val configurationStop = executionStop.getConfiguration()
        configurationStop.node.add(
            "source",
            """
                containerId = "${'$'}{testcontainer.containerid}"
                imageName = "${'$'}{testcontainer.imageName}"
                println("Stopping testcontainer ${'$'}containerId - ${'$'}imageName")
                org.testcontainers.utility.ResourceReaper
                    .instance()
                    .stopAndRemoveContainer(containerId, imageName);
            """
        )


        pluginNode.addDependency("org.testcontainers:postgresql:${'$'}{testcontainers.version}")
    }

    private fun addLiquibasePlugin(pomFile: PomNode) {
        val pluginNode = pomFile.addPluginDependency("org.liquibase:liquibase-maven-plugin:${'$'}{liquibase.version}")
        val execution = pluginNode.addExecution("liquibase-update", BuildPhase.GenerateSources, "update")
        val configuration = execution.getConfiguration().node
        configuration.add("changeLogFile", "src/main/resources/db/master.xml")
        configuration.add("driver", "org.postgresql.Driver")
        configuration.add("url", "${'$'}{db.url}")
        configuration.add("username", "${'$'}{db.username}")
        configuration.add("password", "${'$'}{db.password}")
    }

    private fun addJooqPlugin(pomFile: PomNode, classPath: String) {
        val pluginNode = pomFile.addPluginDependency("org.jooq:jooq-codegen-maven:${'$'}{jooq.version}")
        val execution = pluginNode.addExecution("jooq-codegen", BuildPhase.GenerateSources, "generate")
        val configuration = execution.getConfiguration().node
        val jdbcNode = configuration.add("jdbc")
        jdbcNode.add("url", "${'$'}{db.url}")
        jdbcNode.add("user", "${'$'}{db.username}")
        jdbcNode.add("password", "${'$'}{db.password}")
        val generatorNode = configuration.add("generator")
        val databaseNode = generatorNode.add("database")
        databaseNode.add("inputSchema", "public")
        databaseNode.add("excludes", "databasechangelog|databasechangeloglock")
        val targetNode = generatorNode.add("target")
        targetNode.add("packageName", classPath)
        targetNode.add("directory", "target/generated-sources/jooq")
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

        OutputStreamWriter(file.getOutputStream(), StandardCharsets.UTF_8).use { writer ->
            masterTemplate.merge(velocityContext, writer)
        }
    }

    override fun toString(): String {
        return "Feature[LiquibaseGenerator]"
    }

    private companion object {
        // TODO: get version from configuration
        const val POSTGRESQL_VER = "15.2"
    }
}
