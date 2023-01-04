package com.tezrok.api.feature

import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeProperties

/**
 * [Feature] for creating a new [Node]
 */
interface CreateNodeFeature : Feature {
    /**
     * Creates [Node] by [NodeProperties]
     *
     * Returns null if node cannot be created
     */
    fun createNode(parent: Node, properties: NodeProperties, id: Long): Node?
}
