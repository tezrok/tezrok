package io.tezrok.maven

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

/**
 * Adds Maven default and extra dependencies
 */
internal class MavenCoreFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val projectElem = context.getProject()
        val pomFile = module.pom
        // set project groupId and version
        pomFile.dependencyId = pomFile.dependencyId
            .withGroupId(projectElem.packagePath)
            .withVersion(projectElem.version)

        // add default dependencies
        pomFile.addDependency("org.apache.commons:commons-lang3:3.12.0")
        pomFile.addDependency("org.jetbrains:annotations:24.0.1")
        // add dependencies from project
        projectElem.modules.find { it.name == module.getName() }?.dependencies?.forEach(pomFile::addDependency)

        // add maven-compiler-plugin
        val pluginNode = module.pom.addPluginDependency("org.apache.maven.plugins:maven-compiler-plugin:3.11.0")
        val configuration = pluginNode.getConfiguration().node
        // TODO: get java version from context
        configuration.getOrAdd("source", "17")
        configuration.getOrAdd("target", "17")

        val javaRoot = module.source.main.java
        if (javaRoot.applicationPackageRoot == null) {
            val classPackageRoot = javaRoot.makeDirectories(projectElem.packagePath.replace('.', '/'))
            javaRoot.applicationPackageRoot = classPackageRoot
        }

        return true
    }
}
