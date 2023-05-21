package io.tezrok.maven

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

/**
 * Adds Maven core dependencies
 */
internal class MavenCoreFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        check(project.getModules().size == 1) { "MavenCoreFeature only supports single module" }
        val module = project.getModules().first()
        val projectElem = context.getProject()
        val pomFile = module.pom
        // set project groupId and version
        pomFile.dependencyId = pomFile.dependencyId
            .withGroupId(projectElem.packagePath)
            .withVersion(projectElem.version)

        // add maven-compiler-plugin
        val pluginNode = module.pom.addPluginDependency("org.apache.maven.plugins:maven-compiler-plugin:3.11.0")
        val configuration = pluginNode.getConfiguration().node
        // TODO: get java version from context
        configuration.getOrCreate("source", "1.8")
        configuration.getOrCreate("target", "1.8")

        return true
    }
}
