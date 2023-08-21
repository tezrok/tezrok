package io.tezrok.docker

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

/**
 * Generate docker related files.
 */
internal class DockerFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        // TODO: support sqlite database
        val module = project.getSingleModule()
        val properties = module.properties
        val moduleName = module.getName()
        val dockerName = "${moduleName}Test"
        val values = mapOf(
            "name" to moduleName,
            "userName" to properties.getProperty("datasource.username"),
            "userPassword" to properties.getProperty("datasource.password"),
            "dbName" to properties.getProperty("datasource.db-name"),
            "dockerName" to dockerName,
        )

        val dockerDir = project.getOrAddDirectory("docker")
        val startDbFile = dockerDir.getOrAddFile("start-db.sh")
        if (startDbFile.isEmpty()) {
            context.writeTemplate(startDbFile, "/templates/docker/start-db.sh.vm", values)
        }
        val restartDbFile = dockerDir.getOrAddFile("restart-db.sh")
        if (restartDbFile.isEmpty()) {
            context.writeTemplate(restartDbFile, "/templates/docker/restart-db.sh.vm", values)
        }
        val stopDbFile = dockerDir.getOrAddFile("stop-db.sh")
        if (stopDbFile.isEmpty()) {
            context.writeTemplate(stopDbFile, "/templates/docker/stop-db.sh.vm", values)
        }

        properties.setPropertyIfAbsent("test.docker.name", dockerName)

        return true
    }
}