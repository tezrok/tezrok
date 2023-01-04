package com.tezrok.api.feature

import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeElem

/**
 * Used by a [Feature] to access internal [Node] functionality
 */
interface InternalFeatureSupport {
    /**
     * Returns next unique id for the node
     */
    fun getNextNodeId(): Long

    /**
     * Creates [Node] by [NodeElem]
     */
    fun createNode(parent: Node, nodeElem: NodeElem): Node

    /**
     * Wraps plugin's implementation of [Node] as internal [Node]
     */
    fun wrapNode(node: Node): Node
}
