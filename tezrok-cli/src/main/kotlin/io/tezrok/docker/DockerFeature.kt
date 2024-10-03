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
        val dockerDbName = "$moduleName-test-db"
        val values = mapOf(
            "projectName" to project.getName().toHyphenName(),
            "moduleNameOriginal" to module.getName(),
            "moduleName" to moduleName,
            "dbUserName" to properties.getProperty("datasource.username"),
            "dbUserPassword" to properties.getProperty("datasource.password"),
            "dbName" to properties.getProperty("datasource.db-name"),
            "dockerDbName" to dockerDbName,
            "searchEnabled" to schemaModule.isSearchable()
        )

        context.addFile(project, "/templates/docker/.dockerignore.vm", values)
        val dockerDir = project.getOrAddDirectory("docker")
        context.addFile(dockerDir, "/templates/docker/Dockerfile.vm", values)
        context.addFile(dockerDir, "/templates/docker/app-build-image.sh.vm", values)
        context.addFile(dockerDir, "/templates/docker/app-deploy-common.sh.vm", values)
        val dockerDevDir = dockerDir.getOrAddDirectory("dev")
        context.addFile(dockerDevDir, "/templates/docker/dev/db-run-dev.sh.vm", values)
        context.addFile(dockerDevDir, "/templates/docker/dev/db-stop-dev.sh.vm", values)
        context.addFile(dockerDevDir, "/templates/docker/dev/db-restart-dev.sh.vm", values)
        context.addFile(dockerDevDir, "/templates/docker/dev/app-run-dev.sh.vm", values)
        context.addFile(dockerDevDir, "/templates/docker/dev/app-stop-dev.sh.vm", values)
        context.addFile(dockerDevDir, "/templates/docker/dev/app-deploy-dev.sh.vm", values)
        context.addFile(dockerDevDir, "/templates/docker/dev/secrets.sh.vm", values)
        properties.setPropertyIfAbsent("test.db.docker.name", dockerDbName)

        val customDir = dockerDir.getOrAddDirectory("custom")
        val physicalFile = customDir.getPhysicalPath()?.resolve("dockerenv.sh")
        if (physicalFile == null || !physicalFile.exists()) {
            context.addFile(customDir, "/templates/docker/custom/dockerenv.sh.vm", values)
        }

        return true
    }
}
