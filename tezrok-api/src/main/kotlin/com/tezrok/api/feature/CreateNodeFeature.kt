package com.tezrok.api.feature

import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeProperties
import com.tezrok.api.tree.NodeType
import java.util.function.Supplier

/**
 * [Feature] for creating a new [Node]
 */
interface CreateNodeFeature : Feature {
    /**
     * Creates [Node] by name and [NodeType]
     *
     * Returns null if node cannot be created
     */
    fun createNode(
        parent: Node,
        name: String,
        type: NodeType,
        properties: NodeProperties,
        idGenerator: Supplier<Long>
    ): Node?
}
