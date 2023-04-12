package io.tezrok.api.node

/**
 * Represents a node
 */
interface Node {
    /**
     * Returns the name of the node
     */
    fun getName(): String

    /**
     * Sets the name of the node
     */
    fun setName(name: String)

    /**
     * Returns the parent node
     *
     * null if this is the root node
     */
    fun getParent(): Node?

    fun getChildren(): List<Node>

    /**
     * Returns true if this is the root node
     */
    fun isRoot(): Boolean = getParent() == null

    /**
     * Returns the logical path of the node
     *
     * Logical path based only on the name of the node
     * Parent name not included in path
     */
    fun getPath(): String = getPathTo(null)

    /**
     * Returns path up to the target node
     */
    fun getPathTo(target: Node?): String {
        // Parent name not included in path
        val parent = getParent() ?: return "/"
        val parentPath = if (parent.isRoot() || this == target) "" else parent.getPathTo(target)
        return parentPath + "/" + getName()
    }
}
