package io.tezrok.api.node

/**
 * Represents a resource directory
 */
class ResourcesNode(parent: BaseNode?) : DirectoryNode("resources", parent) {
    override fun setName(name: String) = throw UnsupportedOperationException("Cannot set name for resource node")
}
