package io.tezrok.api.maven

import io.tezrok.api.node.BaseNode
import io.tezrok.api.xml.XmlFileNode

open class PomNode(name: String = "pom.xml", parent: BaseNode? = null) : XmlFileNode(name, "project", parent) {
    @Synchronized
    fun getDependencies(): List<MavenDependency> = TODO()

    @Synchronized
    fun addDependency(dependency: MavenDependency) {
        // TODO: check if dependency already exists

        getXml().getOrCreateNode("dependencies")
            .addNode("dependency")
            .addNode("groupId").setValue(dependency.groupId)
            .addNode("artifactId").setValue(dependency.artifactId)
            .addNode("version").setValue(dependency.version)
    }

    @Synchronized
    fun removeDependency(dependencies: List<MavenDependency>): Boolean = TODO()
}

data class MavenDependency(val groupId: String, val artifactId: String, val version: String)
