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
        get() = getXml().toDependency()
        set(value) = getXml().setDependency(value)

    init {
        getXml().addAttr("xmlns", "http://maven.apache.org/POM/4.0.0")
                .addAttr("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
                .addAttr("xsi:schemaLocation", SCHEMA_LOCATION)
                .getOrAdd("modelVersion", "4.0.0").and()
                .getOrAdd(ARTIFACT_ID, artifactId)
        addProperty("java.version", "17")
        addProperty("project.build.sourceEncoding", "UTF-8")
        addProperty("project.reporting.outputEncoding", "UTF-8")
        // TODO: set name and description
    }

    fun setDescription(description: String): PomNode {
        getXml().getOrAdd("description", description)
        return this
    }

    fun getDescription(): String = getXml().getNodeValue("description")

    fun addProperty(property: MavenProperty): PomNode = addProperty(property.name, property.value)

    fun addProperty(name: String, value: String): PomNode {
        getXml().getOrAdd("properties").getOrAdd(name, value)
        return this
    }

    fun getProperty(name: String): String? = getXml().get("properties")?.getNodeValue(name)

    fun getProperties(): Stream<MavenProperty> = getXml().nodesByPath("properties/*")
            .map { MavenProperty(it.getName(), it.getValue() ?: "") }

    fun setFinalName(finalName: String): PomNode {
        getXml().getOrAdd("build").getOrAdd("finalName", finalName)
        return this
    }

    fun getFinalName(): String = getXml().getNodeValue("build/finalName")

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
            // update scope and version if it is newer
            pluginNode.node.updateVersionAndScope(dependency.version, dependency.scope)
            return pluginNode
        }

        val node: XmlNode = getXml()
                .getOrAdd("build")
                .getOrAdd("plugins")
                .add("plugin")

        node.addDependency(dependency.groupId, dependency.artifactId, dependency.version)

        return PluginNode(node)
    }

    fun getParentNode(): ParentNode = ParentNode(getXml().getOrAdd("parent"))

    fun getModulesRefNode(): ModulesRefNode = ModulesRefNode(getXml().getOrAdd("modules"))

    private fun dependenciesAccess() = MavenDependenciesAccess(getXml())

    private fun pluginNodes(): Stream<PluginNode> = getXml().nodesByPath(PLUGIN_PATH).map { PluginNode(it) }

    companion object {
        const val GROUP_ID = "groupId"
        const val ARTIFACT_ID = "artifactId"
        const val VERSION = "version"
        const val SCOPE = "scope"
        const val PACKAGING = "packaging"
        const val PLUGIN_PATH = "/project/build/plugins/plugin"
        const val SCHEMA_LOCATION = "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
    }
}

internal fun XmlNode.toDependency() = MavenDependency(
    groupId = getNodeValue(PomNode.GROUP_ID),
    artifactId = getNodeValue(PomNode.ARTIFACT_ID),
    version = getNodeValue(PomNode.VERSION),
    scope = getNodeValue(PomNode.SCOPE),
    packaging = getNodeValue(PomNode.PACKAGING)
)

internal fun XmlNode.setDependency(value: MavenDependency) {
    if (value.groupId.isBlank())
        removeAll(PomNode.GROUP_ID)
    else
        getOrAdd(PomNode.GROUP_ID, value.groupId)
    // artifactId is required
    if (value.artifactId.isNotBlank())
        getOrAdd(PomNode.ARTIFACT_ID, value.artifactId)
    if (value.version.isBlank())
        removeAll(PomNode.VERSION)
    else
        getOrAdd(PomNode.VERSION, value.version)
    if (value.scope.isBlank())
        removeAll(PomNode.SCOPE)
    else
        getOrAdd(PomNode.SCOPE, value.scope)
    if (value.packaging.isBlank())
        removeAll(PomNode.PACKAGING)
    else
        getOrAdd(PomNode.PACKAGING, value.packaging)
}

data class MavenProperty(val name: String, val value: String)
