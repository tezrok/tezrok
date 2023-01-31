package com.tezrok.api.node

/**
 * Reference to the node
 */
interface NodeRef {
    /**
     * Full path of the node
     */
    fun getPath(): String

    /**
     * Try to find Node by Path
     */
    fun getNode(): Node?

    /**
     * Return reference to the child node
     *
     * Child node can be not exists
     */
    fun getChild(name: String): NodeRef

    /**
     * Returns true if node accessible
     */
    fun exists(): Boolean = getNode() != null
}
