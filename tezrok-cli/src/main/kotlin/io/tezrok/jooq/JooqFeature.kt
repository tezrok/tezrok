package io.tezrok.jooq

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.BuildPhase
import io.tezrok.api.maven.ModuleNode
import io.tezrok.api.maven.PomNode
import io.tezrok.api.maven.ProjectNode
import org.slf4j.LoggerFactory

/**
 * Add generating of jooq classes from liquibase changelog.
 */
internal class JooqFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val projectElem = context.getProject()
        val module = project.getSingleModule()
        val schemaModule = context.getProject().modules.find { it.name == module.getName() }
            ?: throw IllegalStateException("Module ${module.getName()} not found")
        val schema = schemaModule.schema

        // update pom
        val pomFile = module.pom
        pomFile.addDependency("org.springframework.boot:spring-boot-starter-jooq:${'$'}{spring-boot.version}")
        pomFile.addDependency("org.postgresql:postgresql:42.6.0")
        pomFile.addDependency("org.jooq:jooq:${'$'}{jooq.version}")
        pomFile.addDependency("org.jooq:jooq:${'$'}{jooq.version}")
        pomFile.addDependency("org.testcontainers:postgresql:${'$'}{testcontainers.version}:test")
        pomFile.addProperty("testcontainers.version", "1.19.3")
        pomFile.addProperty("jooq.version", "3.19.0")
        pomFile.addProperty("db.username", "postgres")
        pomFile.addProperty("db.password", "postgres")

        addGroovyPlugin(pomFile)
        addLiquibasePlugin(pomFile)
        addJooqPlugin(pomFile, projectElem.packagePath + ".jooq", schema?.schemaName ?: "public")
        addJooqGenSource(pomFile)

        addJooqConfiguration(module, context)

        return true
    }

    private fun addGroovyPlugin(pomFile: PomNode) {
        val pluginNode = pomFile.addPluginDependency("org.codehaus.gmaven:groovy-maven-plugin:2.1.1")
        val executionStart = pluginNode.addExecution("testcontainer-start", "execute", BuildPhase.GenerateSources)
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

        val executionStop = pluginNode.addExecution("testcontainer-stop", "execute", BuildPhase.Test)
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
        check(
            pomFile.getProperty("liquibase.version")?.isNotBlank()
                ?: false
        ) { "Expected property in maven: liquibase.version" }

        val pluginNode = pomFile.addPluginDependency("org.liquibase:liquibase-maven-plugin:${'$'}{liquibase.version}")
        val execution = pluginNode.addExecution("liquibase-update", "update", BuildPhase.GenerateSources)
        val configuration = execution.getConfiguration().node
        // TODO: get changeLogFile from context
        configuration.add("changeLogFile", "src/main/resources/db/master.xml")
        configuration.add("driver", "org.postgresql.Driver")
        configuration.add("url", "${'$'}{db.url}")
        configuration.add("username", "${'$'}{db.username}")
        configuration.add("password", "${'$'}{db.password}")
    }

    /**
     * Adds build-helper-maven-plugin to add generated sources to the project.
     */
    private fun addJooqGenSource(pomFile: PomNode) {
        val pluginNode = pomFile.addPluginDependency("org.codehaus.mojo:build-helper-maven-plugin:3.5.0")
        val execution = pluginNode.addExecution("add-source", "add-source", BuildPhase.GenerateSources)
        val configuration = execution.getConfiguration().node
        configuration.add("sources")
            .add("source")
            .setValue("${'$'}{basedir}/target/generated-sources/jooq")
    }

    private fun addJooqPlugin(pomFile: PomNode, classPath: String, schemaName: String) {
        val pluginNode = pomFile.addPluginDependency("org.jooq:jooq-codegen-maven:${'$'}{jooq.version}")
        val execution = pluginNode.addExecution("jooq-codegen", "generate", BuildPhase.GenerateSources)
        val configuration = execution.getConfiguration().node
        val jdbcNode = configuration.add("jdbc")
        jdbcNode.add("url", "${'$'}{db.url}")
        jdbcNode.add("user", "${'$'}{db.username}")
        jdbcNode.add("password", "${'$'}{db.password}")
        val generatorNode = configuration.add("generator")
        val databaseNode = generatorNode.add("database")
        databaseNode.add("inputSchema", schemaName)
        databaseNode.add("excludes", "databasechangelog|databasechangeloglock")
        val generateNode = generatorNode.add("generate")
        generateNode.add("pojos", "true")
        generateNode.add("pojosEqualsAndHashCode", "true")
        generateNode.add("fluentSetters", "true")
        generateNode.add("javaTimeTypes", "true")
        val targetNode = generatorNode.add("target")
        targetNode.add("packageName", classPath)
        targetNode.add("directory", "target/generated-sources/jooq")
    }

    private fun addJooqConfiguration(module: ModuleNode, context: GeneratorContext) {
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot
        if (applicationPackageRoot != null) {
            val configDir = applicationPackageRoot.getOrAddJavaDirectory("config")
            val configPackage = context.getProject().packagePath + ".config"
            val values = mapOf("package" to configPackage)

            val jooqConfig = configDir.getOrAddFile("JooqConfiguration.java")
            if (jooqConfig.isEmpty()) {
                context.writeTemplate(jooqConfig, "/templates/jooq/JooqConfiguration.java.vm", values)
            } else {
                log.warn("JooqConfiguration.java already exists, skipping")
            }

            val exceptionTranslator = configDir.getOrAddFile("ExceptionTranslator.java")
            if (exceptionTranslator.isEmpty()) {
                context.writeTemplate(exceptionTranslator, "/templates/jooq/ExceptionTranslator.java.vm", values)
            } else {
                log.warn("ExceptionTranslator.java already exists, skipping")
            }
        }
    }

    private companion object {
        // TODO: get version from configuration
        const val POSTGRESQL_VER = "15.2"
        val log = LoggerFactory.getLogger(JooqFeature::class.java)!!
    }
}