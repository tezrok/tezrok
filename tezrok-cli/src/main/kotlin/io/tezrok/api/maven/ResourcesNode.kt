package io.tezrok.api.maven

import io.tezrok.api.node.BaseNode
import io.tezrok.api.node.DirectoryNode

/**
 * Represents a resource directory
 */
class ResourcesNode(parent: BaseNode?) : DirectoryNode("resources", parent) {
    override fun setName(name: String) = throw UnsupportedOperationException("Cannot set name for resource node")
}
