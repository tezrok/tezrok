package io.tezrok.api.maven

import io.tezrok.api.xml.XmlNode
import io.tezrok.util.find
import java.util.stream.Stream

/**
 * Implementation of [MavenDependencies] interface
 */
internal class MavenDependenciesAccess(val parent: XmlNode) : MavenDependencies {
    override fun getDependencies(): Stream<MavenDependency> = dependencyNodes().map { it.toDependency() }

    /**
     * Get maven dependency by groupId and artifactId
     */
    override fun getDependency(groupId: String, artifactId: String): MavenDependency? =
            getDependencies().find { it.groupId == groupId && it.artifactId == artifactId }

    override fun addDependency(dependency: MavenDependency): Boolean {
        val groupId = dependency.groupId
        val artifactId = dependency.artifactId
        val version = dependency.version
        val shortId = dependency.shortId()

        val dependencyNode = dependencyNodes().find { node -> node.shortId() == shortId }

        if (dependencyNode != null) {
            // if dependency already exists, check if version is newer
            return dependencyNode.updateVersion(version)
        }

        parent.getOrAdd("dependencies")
                .add("dependency")
                .addDependency(groupId, artifactId, version)

        return true
    }

    /**
     * Remove maven dependencies
     */
    override fun removeDependencies(dependencies: List<MavenDependency>): Boolean {
        val shortIdsToRemove = dependencies.map { it.shortId() }.toHashSet()
        val dependencyNodes = dependencyNodes()
                .filter { node -> shortIdsToRemove.contains(node.shortId()) }
                .toList()

        return parent.get("dependencies")?.remove(dependencyNodes) ?: false
    }

    private fun dependencyNodes(): Stream<XmlNode> = parent.nodesByPath("dependencies/dependency")
}

internal fun XmlNode.shortId(): String = getNodeValue(PomNode.GROUP_ID) + ":" + getNodeValue(PomNode.ARTIFACT_ID)

internal fun XmlNode.addDependency(groupId: String, artifactId: String, version: String) {
    add(PomNode.GROUP_ID, groupId).and()
            .add(PomNode.ARTIFACT_ID, artifactId)

    if (version.isNotBlank()) {
        add(PomNode.VERSION, version)
    }
}

/**
 * Update dependency version if it is newer
 */
internal fun XmlNode.updateVersion(version: String): Boolean {
    if (version.isNotBlank()) {
        val versionNode = getOrAdd(PomNode.VERSION)

        if (versionNode.getValue()?.isBlank() == true || version > versionNode.getValue()!!) {
            versionNode.setValue(version)
            return true
        }
    }
    return false
}
