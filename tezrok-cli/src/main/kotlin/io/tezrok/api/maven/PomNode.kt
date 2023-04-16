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
            .getOrCreate("artifactId").setValue(artifactId)
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

    private fun setDependencyIdInternal(value: MavenDependency) {
        val xml = getXml()
        if (value.groupId.isBlank())
            xml.removeAll("groupId")
        else
            xml.getOrCreate("groupId").setValue(value.groupId)
        // artifactId is required
        if (value.artifactId.isNotBlank())
            xml.getOrCreate("artifactId").setValue(value.artifactId)
        if (value.version.isBlank())
            xml.removeAll("version")
        else
            xml.getOrCreate("version").setValue(value.version)
    }

    private fun getDependencyIdInternal(): MavenDependency = getXml().let { xml ->
        MavenDependency(
            xml.getNodeValue("groupId"),
            xml.getNodeValue("artifactId"),
            xml.getNodeValue("version")
        )
    }

    private companion object {
        const val DEPENDENCY_PATH = "/project/dependencies/dependency"
        const val SCHEMA_LOCATION = "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
    }
}

data class MavenDependency(val groupId: String, val artifactId: String, val version: String) {
    fun shortId(): String = "$groupId:$artifactId"

    fun fullId(): String = "$groupId:$artifactId:$version"

    companion object {
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
