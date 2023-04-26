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
    XmlFileNode(name, "project", parent), MavenDependencies {
    var dependencyId: MavenDependency
        get() = getDependencyIdInternal()
        set(value) = setDependencyIdInternal(value)

    init {
        getXml().addAttr("xmlns", "http://maven.apache.org/POM/4.0.0")
            .addAttr("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
            .addAttr("xsi:schemaLocation", SCHEMA_LOCATION)
            .getOrCreate("modelVersion").setValue("4.0.0").and()
            .getOrCreate(ARTIFACT_ID).setValue(artifactId)
        addProperty("java.version", "17")
        addProperty("project.build.sourceEncoding", "UTF-8")
        addProperty("project.reporting.outputEncoding", "UTF-8")
    }

    fun addProperty(property: MavenProperty): PomNode = addProperty(property.name, property.value)

    fun addProperty(name: String, value: String): PomNode {
        getXml().getOrCreate("properties").getOrCreate(name).setValue(value)
        return this
    }

    fun getProperty(name: String): String? = getXml().get("properties")?.getNodeValue(name)

    fun getProperties(): Stream<MavenProperty> = getXml().nodesByPath("properties/*")
        .map { MavenProperty(it.getName(), it.getValue() ?: "") }

    override fun getDependencies(): Stream<MavenDependency> = dependenciesAccess().getDependencies()

    /**
     * Get maven dependency by groupId and artifactId
     */
    override fun getDependency(groupId: String, artifactId: String): MavenDependency? =
        dependenciesAccess().getDependency(groupId, artifactId)

    /**
     * Add maven dependency or update existing one if version is newer
     *
     * @return true if dependency was added or updated
     */
    override fun addDependency(dependency: String): Boolean = dependenciesAccess().addDependency(dependency)

    /**
     * Add maven dependency or update existing one if version is newer
     *
     * @return true if dependency was added or updated
     */
    override fun addDependency(dependency: MavenDependency): Boolean = dependenciesAccess().addDependency(dependency)

    /**
     * Remove maven dependencies
     */
    override fun removeDependencies(dependencies: List<MavenDependency>): Boolean =
        dependenciesAccess().removeDependencies(dependencies)

    fun addPluginDependency(dependency: String): PluginNode = addPluginDependency(MavenDependency.of(dependency))

    fun addPluginDependency(dependency: MavenDependency): PluginNode {
        val pluginNode = pluginNodes().find { node -> node.dependency.shortId() == dependency.shortId() }

        // if plugin already exists
        if (pluginNode != null) {
            // update version if it is newer
            pluginNode.node.updateVersion(dependency.version)
            return pluginNode
        }

        val node: XmlNode = getXml()
            .getOrCreate("build")
            .getOrCreate("plugins")
            .add("plugin")

        node.addDependency(dependency.groupId, dependency.artifactId, dependency.version)

        return PluginNode(node)
    }

    private fun dependenciesAccess() = MavenDependenciesAccess(getXml())

    private fun pluginNodes(): Stream<PluginNode> = getXml().nodesByPath(PLUGIN_PATH).map { PluginNode(it) }

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

    companion object {
        const val GROUP_ID = "groupId"
        const val ARTIFACT_ID = "artifactId"
        const val VERSION = "version"
        const val PLUGIN_PATH = "/project/build/plugins/plugin"
        const val SCHEMA_LOCATION = "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
    }
}

internal fun XmlNode.toDependency() = MavenDependency(
    groupId = getNodeValue(PomNode.GROUP_ID),
    artifactId = getNodeValue(PomNode.ARTIFACT_ID),
    version = getNodeValue(PomNode.VERSION)
)

data class MavenProperty(val name: String, val value: String)
