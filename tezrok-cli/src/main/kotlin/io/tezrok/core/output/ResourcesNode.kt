package io.tezrok.core.output

import io.tezrok.core.common.BaseNode
import io.tezrok.core.common.DirectoryNode

/**
 * Represents a resource directory
 */
class ResourcesNode(parent: BaseNode?) : DirectoryNode("resources", parent) {
    override fun setName(name: String) = throw UnsupportedOperationException("Cannot set name for resource node")
}
