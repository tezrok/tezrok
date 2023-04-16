package io.tezrok.api.maven

import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.BaseNode
import io.tezrok.api.node.DirectoryNode

/**
 * Represents a module. Which represents a separate maven module
 * which can be a library or a web application
 */
open class ModuleNode(name: String, parent: BaseNode?) : DirectoryNode(name, parent) {
    val resources: ResourcesNode = ResourcesNode(this)

    val pom: PomNode = PomNode(artifactId = name, parent = this)

    override fun getFiles(): List<BaseFileNode> = listOf(pom, resources)
}
