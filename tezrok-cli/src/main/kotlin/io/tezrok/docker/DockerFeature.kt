package io.tezrok.docker

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.toHyphenName
import kotlin.io.path.exists

/**
 * Generate docker related files.
 */
internal class DockerFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        // TODO: support sqlite database
        val module = project.getSingleModule()
        val schemaModule = context.getProject().modules.find { it.name == module.getName() }
            ?: throw IllegalStateException("Module ${module.getName()} not found")
        val properties = module.properties
        val moduleName = module.getName().toHyphenName()
        val dockerTestDbName = "$moduleName-test-db"
        val testValues = mapOf(
            "projectName" to project.getName().toHyphenName(),
            "moduleNameOriginal" to module.getName(),
            "moduleName" to moduleName,
            "dbUserName" to properties.getProperty("datasource.username"),
            "dbUserPassword" to properties.getProperty("datasource.password"),
            "dbName" to properties.getProperty("datasource.db-name"),
            "dockerDbName" to dockerTestDbName,
            "searchEnabled" to schemaModule.isSearchable()
        )

        context.addFile(project, "/templates/docker/.dockerignore.vm", testValues)
        val dockerDir = project.getOrAddDirectory("docker")
        context.addFile(dockerDir, "/templates/docker/Dockerfile.vm", testValues)
        context.addFile(dockerDir, "/templates/docker/app-build-image.sh.vm", testValues)
        context.addFile(dockerDir, "/templates/docker/app-deploy-common.sh.vm", testValues)
        val dockerDevDir = dockerDir.getOrAddDirectory("dev")
        context.addFile(dockerDevDir, "/templates/docker/dev/db-run-dev.sh.vm", testValues)
        context.addFile(dockerDevDir, "/templates/docker/dev/db-stop-dev.sh.vm", testValues)
        context.addFile(dockerDevDir, "/templates/docker/dev/db-restart-dev.sh.vm", testValues)
        context.addFile(dockerDevDir, "/templates/docker/dev/app-run-dev.sh.vm", testValues)
        context.addFile(dockerDevDir, "/templates/docker/dev/app-stop-dev.sh.vm", testValues)
        context.addFile(dockerDevDir, "/templates/docker/dev/app-deploy-dev.sh.vm", testValues)
        context.addFile(dockerDevDir, "/templates/docker/dev/secrets.sh.vm", testValues)
        context.addFile(dockerDevDir, "/templates/docker/dev/variables.env")
        properties.setPropertyIfAbsent("test.db.docker.name", dockerTestDbName)

        val dockerProdDbName = "$moduleName-db"
        val prodValues = mapOf(
            "projectName" to project.getName().toHyphenName(),
            "moduleNameOriginal" to module.getName(),
            "moduleName" to moduleName,
            "dbUserName" to properties.getProperty("datasource.username"),
            "dbUserPassword" to properties.getProperty("datasource.password"),
            "dbName" to properties.getProperty("datasource.db-name"),
            "dockerDbName" to dockerProdDbName,
            "searchEnabled" to schemaModule.isSearchable()
        )

        val dockerProdDir = dockerDir.getOrAddDirectory("prod")
        context.addFile(dockerProdDir, "/templates/docker/prod/db-run.sh.vm", prodValues)
        context.addFile(dockerProdDir, "/templates/docker/prod/db-stop.sh.vm", prodValues)
        context.addFile(dockerProdDir, "/templates/docker/prod/db-restart.sh.vm", prodValues)
        context.addFile(dockerProdDir, "/templates/docker/prod/app-run.sh.vm", prodValues)
        context.addFile(dockerProdDir, "/templates/docker/prod/app-stop.sh.vm", prodValues)
        context.addFile(dockerProdDir, "/templates/docker/prod/app-deploy.sh.vm", prodValues)
        context.addFile(dockerProdDir, "/templates/docker/prod/secrets.sh.vm", prodValues)
        context.addFile(dockerProdDir, "/templates/docker/dev/variables.env")
        properties.setPropertyIfAbsent("prod.db.docker.name", dockerProdDbName)

        val customDir = dockerDir.getOrAddDirectory("custom")
        val physicalFile = customDir.getPhysicalPath()?.resolve("dockerenv.sh")
        if (physicalFile == null || !physicalFile.exists()) {
            context.addFile(customDir, "/templates/docker/custom/dockerenv.sh.vm", testValues)
        }

        return true
    }
}
