package com.tezrok.api.tree

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
     * Returns true if node accessible
     */
    fun exists(): Boolean = getNode() != null
}
