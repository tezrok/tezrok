package io.tezrok.api.maven

import io.tezrok.api.node.DirectoryNode
import io.tezrok.api.node.Node

/**
 * Represents a resources directory
 */
class ResourcesNode(parent: Node?) : DirectoryNode("resources", parent) {
    override fun setName(name: String) = throw UnsupportedOperationException("Cannot set name for resource node")
}
