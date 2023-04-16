package io.tezrok.api.maven

import io.tezrok.api.node.BaseNode
import io.tezrok.api.xml.XmlFileNode
import io.tezrok.api.xml.XmlNode


/**
 * Class works with maven pom.xml
 */
open class PomNode(name: String = "pom.xml", parent: BaseNode? = null) : XmlFileNode(name, "project", parent) {
    @Synchronized
    fun getDependencies(): List<MavenDependency> = getXml()
        .nodesByPath(DEPENDENCY_PATH)
        .map { it.toDependency() }

    /**
     * Add maven dependency or update existing one if version is newer
     */
    @Synchronized
    fun addDependency(dependency: MavenDependency): Boolean {
        val groupId = dependency.groupId
        val artifactId = dependency.artifactId
        val version = dependency.version

        var dependencyNode = getXml()
            .nodesByPath("/project/dependencies/dependency[groupId='$groupId' and artifactId='$artifactId']")
            .firstOrNull()

        if (dependencyNode != null) {
            // if dependency already exists, check if version is newer
            if (version.isNotBlank()) {
                val versionNode = dependencyNode.getOrCreate("version")
                if (versionNode.getValue()?.isBlank() == true || version > versionNode.getValue()!!) {
                    versionNode.setValue(version)
                    return true
                }
            }
            return false
        }

        dependencyNode = getXml().getOrCreate("dependencies")
            .add("dependency")
            .add("groupId").setValue(groupId).and()
            .add("artifactId").setValue(artifactId).and()

        if (version.isNotBlank()) {
            dependencyNode.add("version").setValue(version)
        }

        return true
    }

    /**
     * Remove maven dependencies
     */
    @Synchronized
    fun removeDependencies(dependencies: List<MavenDependency>): Boolean {
        val dependenciesToRemove = dependencies.map { it.groupId + ":" + it.artifactId }.toHashSet()
        val dependencyNodes = getXml().nodesByPath(DEPENDENCY_PATH)
            .filter { node -> dependenciesToRemove.contains(node.shortId()) }

        return getXml().remove(dependencyNodes)
    }

    private fun XmlNode.shortId(): String =
        (get("groupId")?.getValue() ?: "") + ":" + (get("artifactId")?.getValue() ?: "")

    private fun XmlNode.toDependency() = MavenDependency(
        groupId = this.get("groupId")?.getValue() ?: "",
        artifactId = this.get("artifactId")?.getValue() ?: "",
        version = this.get("version")?.getValue() ?: ""
    )

    private companion object {
        const val DEPENDENCY_PATH = "/project/dependencies/dependency"
    }
}

data class MavenDependency(val groupId: String, val artifactId: String, val version: String)
