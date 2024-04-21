package io.tezrok.docker

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.NameUtil
import kotlin.io.path.exists

/**
 * Generate docker related files.
 */
internal class DockerFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        // TODO: support sqlite database
        val module = project.getSingleModule()
        val properties = module.properties
        val moduleName = NameUtil.toHyphenName(module.getName())
        val dockerDbName = "$moduleName-test-db"
        val values = mapOf(
            "projectName" to NameUtil.toHyphenName(project.getName()),
            "moduleNameOriginal" to module.getName(),
            "moduleName" to moduleName,
            "dbUserName" to properties.getProperty("datasource.username"),
            "dbUserPassword" to properties.getProperty("datasource.password"),
            "dbName" to properties.getProperty("datasource.db-name"),
            "dockerDbName" to dockerDbName
        )

        context.addFile(project, "/templates/docker/.dockerignore.vm", values)
        val dockerDir = project.getOrAddDirectory("docker")
        context.addFile(dockerDir, "/templates/docker/db-start-test.sh.vm", values)
        context.addFile(dockerDir, "/templates/docker/db-stop-test.sh.vm", values)
        context.addFile(dockerDir, "/templates/docker/db-restart-test.sh.vm", values)
        context.addFile(dockerDir, "/templates/docker/app-build-image.sh.vm", values)
        context.addFile(dockerDir, "/templates/docker/app-run-test.sh.vm", values)
        context.addFile(dockerDir, "/templates/docker/app-stop-test.sh.vm", values)
        context.addFile(dockerDir, "/templates/docker/app-deploy-common.sh.vm", values)
        context.addFile(dockerDir, "/templates/docker/app-deploy-dev.sh.vm", values)
        context.addFile(dockerDir, "/templates/docker/Dockerfile.vm", values)
        properties.setPropertyIfAbsent("test.db.docker.name", dockerDbName)

        val customDir = dockerDir.getOrAddDirectory("custom")
        val physicalFile = customDir.getPhysicalPath()?.resolve("dockerenv.sh")
        if (physicalFile == null || !physicalFile.exists()) {
            context.addFile(customDir, "/templates/docker/dockerenv.sh.vm", values)
        }

        return true
    }
}
