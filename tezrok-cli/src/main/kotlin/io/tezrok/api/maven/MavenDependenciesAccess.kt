package io.tezrok.api.maven

import io.tezrok.api.xml.XmlNode
import io.tezrok.util.find
import java.util.stream.Stream

/**
 * Helper class to manage maven dependencies node
 */
internal class MavenDependenciesAccess(private val lockObject: Any, val parent: XmlNode) {
    fun getDependencies(): Stream<MavenDependency> = synchronized(lockObject) {
        // should call toList() due to Stream laziness
        getDependenciesInternal().toList().stream()
    }

    /**
     * Get maven dependency by groupId and artifactId
     */
    fun getDependency(groupId: String, artifactId: String): MavenDependency? = synchronized(lockObject) {
        getDependenciesInternal().find { it.groupId == groupId && it.artifactId == artifactId }
    }

    fun addDependency(dependency: String): Boolean = addDependency(MavenDependency.of(dependency))

    fun addDependency(dependency: MavenDependency): Boolean = synchronized(lockObject) {
        val groupId = dependency.groupId
        val artifactId = dependency.artifactId
        val version = dependency.version
        val shortId = dependency.shortId()

        val dependencyNode = dependencyNodes().find { node -> node.shortId() == shortId }

        if (dependencyNode != null) {
            // if dependency already exists, check if version is newer
            return dependencyNode.updateVersion(version)
        }

        parent.getOrCreate("dependencies")
            .add("dependency")
            .addDependency(groupId, artifactId, version)

        return true
    }

    /**
     * Remove maven dependencies
     */
    fun removeDependencies(dependencies: List<MavenDependency>): Boolean = synchronized(lockObject) {
        val shortIdsToRemove = dependencies.map { it.shortId() }.toHashSet()
        val dependencyNodes = dependencyNodes()
            .filter { node -> shortIdsToRemove.contains(node.shortId()) }
            .toList()

        return parent.get("dependencies")?.remove(dependencyNodes) ?: false
    }

    private fun getDependenciesInternal(): Stream<MavenDependency> = dependencyNodes().map { it.toDependency() }

    private fun dependencyNodes(): Stream<XmlNode> = parent.nodesByPath("dependencies/dependency")
}

internal fun XmlNode.shortId(): String = getNodeValue(PomNode.GROUP_ID) + ":" + getNodeValue(PomNode.ARTIFACT_ID)

internal fun XmlNode.addDependency(groupId: String, artifactId: String, version: String) {
    add(PomNode.GROUP_ID).setValue(groupId).and()
        .add(PomNode.ARTIFACT_ID).setValue(artifactId)

    if (version.isNotBlank()) {
        add(PomNode.VERSION).setValue(version)
    }
}

/**
 * Update dependency version if it is newer
 */
internal fun XmlNode.updateVersion(version: String): Boolean {
    if (version.isNotBlank()) {
        val versionNode = getOrCreate(PomNode.VERSION)

        if (versionNode.getValue()?.isBlank() == true || version > versionNode.getValue()!!) {
            versionNode.setValue(version)
            return true
        }
    }
    return false
}
