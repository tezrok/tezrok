package io.tezrok.api.maven

import io.tezrok.api.node.BaseNode
import io.tezrok.api.xml.XmlFileNode
import io.tezrok.api.xml.XmlNode
import io.tezrok.util.find
import java.util.stream.Stream

/**
 * Class works with maven pom.xml
 */
open class PomNode(artifactId: String, name: String = "pom.xml", parent: BaseNode? = null) :
    XmlFileNode(name, "project", parent) {
    var dependencyId: MavenDependency
        get() = getDependencyIdInternal()
        set(value) = setDependencyIdInternal(value)

    init {
        getXml().addAttr("xmlns", "http://maven.apache.org/POM/4.0.0")
            .addAttr("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
            .addAttr("xsi:schemaLocation", SCHEMA_LOCATION)
            .getOrCreate("modelVersion").setValue("4.0.0").and()
            .getOrCreate(ARTIFACT_ID).setValue(artifactId)
    }

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
    fun addDependency(dependency: String): Boolean = addDependency(MavenDependency.of(dependency))

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
                val versionNode = dependencyNode.getOrCreate(VERSION)
                if (versionNode.getValue()?.isBlank() == true || version > versionNode.getValue()!!) {
                    versionNode.setValue(version)
                    return true
                }
            }
            return false
        }

        dependencyNode = getXml().getOrCreate("dependencies")
            .add("dependency")
            .add(GROUP_ID).setValue(groupId).and()
            .add(ARTIFACT_ID).setValue(artifactId).and()

        if (version.isNotBlank()) {
            dependencyNode.add(VERSION).setValue(version)
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

    private fun XmlNode.shortId(): String = getNodeValue(GROUP_ID) + ":" + getNodeValue(ARTIFACT_ID)

    private fun XmlNode.toDependency() = MavenDependency(
        groupId = getNodeValue(GROUP_ID),
        artifactId = getNodeValue(ARTIFACT_ID),
        version = getNodeValue(VERSION)
    )

    private fun setDependencyIdInternal(value: MavenDependency) {
        val xml = getXml()
        if (value.groupId.isBlank())
            xml.removeAll(GROUP_ID)
        else
            xml.getOrCreate(GROUP_ID).setValue(value.groupId)
        // artifactId is required
        if (value.artifactId.isNotBlank())
            xml.getOrCreate(ARTIFACT_ID).setValue(value.artifactId)
        if (value.version.isBlank())
            xml.removeAll(VERSION)
        else
            xml.getOrCreate(VERSION).setValue(value.version)
    }

    private fun getDependencyIdInternal(): MavenDependency = getXml().let { xml ->
        MavenDependency(
            xml.getNodeValue(GROUP_ID),
            xml.getNodeValue(ARTIFACT_ID),
            xml.getNodeValue(VERSION)
        )
    }

    private companion object {
        const val GROUP_ID = "groupId"
        const val ARTIFACT_ID = "artifactId"
        const val VERSION = "version"
        const val DEPENDENCY_PATH = "/project/dependencies/dependency"
        const val SCHEMA_LOCATION = "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
    }
}

data class MavenDependency(val groupId: String, val artifactId: String, val version: String) {
    fun shortId(): String = "$groupId:$artifactId"

    fun fullId(): String = "$groupId:$artifactId:$version"

    fun withGroupId(groupId: String): MavenDependency = MavenDependency(groupId, artifactId, version)

    fun withArtifactId(artifactId: String): MavenDependency = MavenDependency(groupId, artifactId, version)

    fun withVersion(version: String): MavenDependency = MavenDependency(groupId, artifactId, version)

    companion object {
        /**
         * Parse maven dependency from string
         *
         * @param dependency dependency in format "groupId:artifactId:version"
         */
        @JvmStatic
        fun of(dependency: String): MavenDependency {
            val parts = dependency.split(":")
            return MavenDependency(
                groupId = parts[0],
                artifactId = parts[1],
                version = if (parts.size > 2) parts[2] else ""
            )
        }
    }
}
