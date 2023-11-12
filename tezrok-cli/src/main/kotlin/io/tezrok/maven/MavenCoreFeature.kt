package io.tezrok.maven

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.ProjectElem
import io.tezrok.api.maven.PomNode
import io.tezrok.api.maven.ProjectNode

/**
 * Adds Maven default and extra dependencies
 */
internal class MavenCoreFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val projectElem = context.getProject()
        val parentPomDependency = fillParentPom(project, projectElem).dependencyId

        val pomFile = module.pom
        // set project groupId and version
        pomFile.dependencyId = pomFile.dependencyId
            .withGroupId(projectElem.packagePath)
            .withVersion(projectElem.version)

        pomFile.getParentNode().dependencyId = pomFile.getParentNode().dependencyId
            .withGroupId(parentPomDependency.groupId)
            .withArtifactId(parentPomDependency.artifactId)
            .withVersion(parentPomDependency.version)

        // add default properties
        pomFile.addProperty("commons-lang3.version", "3.12.0")
        pomFile.addProperty("logback.version", "1.4.6")
        pomFile.addProperty("slf4j-api.version", "2.0.5")
        pomFile.addProperty("junit.version", "5.8.1")
        pomFile.addProperty("lombok.version", "1.18.28")

        // add default dependencies
        pomFile.addDependency("org.apache.commons:commons-lang3:${'$'}{commons-lang3.version}")
        pomFile.addDependency("org.jetbrains:annotations:24.0.1")
        pomFile.addDependency("org.projectlombok:lombok:${'$'}{lombok.version}")
        // add logging dependencies
        pomFile.addDependency("ch.qos.logback:logback-classic:${'$'}{logback.version}")
        pomFile.addDependency("org.slf4j:slf4j-api:${'$'}{slf4j-api.version}")
        // add testing dependencies
        pomFile.addDependency("org.junit.jupiter:junit-jupiter-api:${'$'}{junit.version}:test")
        pomFile.addDependency("org.junit.jupiter:junit-jupiter-engine:${'$'}{junit.version}:test")
        pomFile.addDependency("org.junit.platform:junit-platform-launcher:1.9.0:test")
        pomFile.addDependency("org.mockito:mockito-core:5.2.0:test");

        // add dependencies from project
        projectElem.modules.find { it.name == module.getName() }?.dependencies?.forEach(pomFile::addDependency)

        addMavenCompilerPlugin(pomFile, true)

        val javaRoot = module.source.main.java
        if (javaRoot.applicationPackageRoot == null) {
            val classPackageRoot = javaRoot.makeDirectories(projectElem.packagePath.replace('.', '/'))
            javaRoot.applicationPackageRoot = classPackageRoot
        }

        return true
    }

    private fun fillParentPom(project: ProjectNode, projectElem: ProjectElem): PomNode {
        val pomFile = project.pom

        if (pomFile.dependencyId.groupId.isBlank()) {
            pomFile.dependencyId = pomFile.dependencyId
                .withGroupId(projectElem.packagePath)
                .withVersion(projectElem.version)
        }
        addMavenCompilerPlugin(pomFile)

        return project.pom
    }

    /**
     * Add maven-compiler-plugin to pom.xml
     */
    private fun addMavenCompilerPlugin(pomFile: PomNode, configPath: Boolean = false) {
        val pluginNode = pomFile.addPluginDependency("org.apache.maven.plugins:maven-compiler-plugin:3.11.0")
        val configuration = pluginNode.getConfiguration().node
        // TODO: get java version from context
        configuration.getOrAdd("source", "17")
        configuration.getOrAdd("target", "17")

        if (configPath) {
            configuration.getOrAdd("annotationProcessorPaths")
                .add("path")
                .getOrAdd("groupId", "org.projectlombok").and()
                .getOrAdd("artifactId", "lombok").and()
                .getOrAdd("version", "${'$'}{lombok.version}")
        }
    }
}
