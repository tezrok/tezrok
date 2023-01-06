package com.tezrok.api.feature

import com.tezrok.api.tree.Node

/**
 * Used by a [Feature] to access internal [Node] functionality
 */
interface InternalFeatureSupport {
    /**
     * Returns next unique id for the node
     */
    fun getNextNodeId(): Long

    /**
     * Apply some internal changes to the [Node]
     *
     * TODO: Add more information
     */
    fun applyNode(node: Node): Boolean
}
