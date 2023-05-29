package io.tezrok.jooq

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.BuildPhase
import io.tezrok.api.maven.PomNode
import io.tezrok.api.maven.ProjectNode

/**
 * Add generating of jooq classes from liquibase changelog.
 */
internal class JooqGenerator : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val projectElem = context.getProject()
        val module = project.getSingleModule()
        // update pom
        val pomFile = module.pom
        pomFile.addDependency("org.postgresql:postgresql:42.6.0")
        pomFile.addDependency("org.jooq:jooq:${'$'}{jooq.version}")
        pomFile.addProperty("liquibase.version", "3.8.9")
        pomFile.addProperty("testcontainers.version", "1.18.0")
        pomFile.addProperty("jooq.version", "3.13.4")
        pomFile.addProperty("db.username", "postgres")
        pomFile.addProperty("db.password", "postgres")

        addGroovyPlugin(pomFile)
        addLiquibasePlugin(pomFile)
        addJooqPlugin(pomFile, projectElem.packagePath + ".jooq")

        return true
    }

    private fun addGroovyPlugin(pomFile: PomNode) {
        val pluginNode = pomFile.addPluginDependency("org.codehaus.gmaven:groovy-maven-plugin:2.1.1")
        val executionStart = pluginNode.addExecution("testcontainer-start", BuildPhase.GenerateSources, "execute")
        val configurationStart = executionStart.getConfiguration()
        configurationStart.node.add(
            "source",
            """
                db = new org.testcontainers.containers.PostgreSQLContainer("postgres:${POSTGRESQL_VER}")
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
        // TODO: get changeLogFile from context
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

    private companion object {
        // TODO: get version from configuration
        const val POSTGRESQL_VER = "15.2"
    }
}