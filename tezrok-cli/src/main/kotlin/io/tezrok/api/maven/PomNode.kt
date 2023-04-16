package io.tezrok.api.maven

import io.tezrok.api.node.BaseNode
import io.tezrok.api.xml.XmlFileNode
import io.tezrok.api.xml.XmlNode
import io.tezrok.util.find
import java.util.stream.Stream


/**
 * Class works with maven pom.xml
 */
open class PomNode(name: String = "pom.xml", parent: BaseNode? = null) : XmlFileNode(name, "project", parent) {
    @Synchronized
    fun getDependencies(): Stream<MavenDependency> = dependencyNodes().map { it.toDependency() }

    /**
     * Get maven dependency by groupId and artifactId
     */
    @Synchronized
    fun getDependency(groupId: String, artifactId: String): MavenDependency? = getDependencies()
        .find { it.groupId == groupId && it.artifactId == artifactId }

    /**
     * Add maven dependency or update existing one if version is newer
     *
     * @return true if dependency was added or updated
     */
    @Synchronized
    fun addDependency(dependency: MavenDependency): Boolean {
        val groupId = dependency.groupId
        val artifactId = dependency.artifactId
        val version = dependency.version
        val shortId = dependency.shortId()

        var dependencyNode = dependencyNodes().find { node -> node.shortId() == shortId }

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
        val shortIdsToRemove = dependencies.map { it.shortId() }.toHashSet()
        val dependencyNodes = dependencyNodes()
            .filter { node -> shortIdsToRemove.contains(node.shortId()) }
            .toList()

        return getXml().remove(dependencyNodes)
    }

    private fun dependencyNodes() = getXml().nodesByPath(DEPENDENCY_PATH)

    private fun XmlNode.shortId(): String = getNodeValue("groupId") + ":" + getNodeValue("artifactId")

    private fun XmlNode.toDependency() = MavenDependency(
        groupId = getNodeValue("groupId"),
        artifactId = getNodeValue("artifactId"),
        version = getNodeValue("version")
    )

    private companion object {
        const val DEPENDENCY_PATH = "/project/dependencies/dependency"
    }
}

data class MavenDependency(val groupId: String, val artifactId: String, val version: String) {
    fun shortId(): String = "$groupId:$artifactId"

    fun fullId(): String = "$groupId:$artifactId:$version"
}
