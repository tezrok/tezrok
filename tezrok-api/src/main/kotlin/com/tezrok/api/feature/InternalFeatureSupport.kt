package com.tezrok.api.feature

import com.tezrok.api.event.EventResult
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeType
import java.util.function.Function

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

    /**
     * Add subscriber to the event
     */
    fun subscribeOnEvent(type: NodeType, handler: Function<NodeEvent, EventResult>)

    /**
     * Remove subscriber
     */
    fun unsubscribeOnEvent(handler: Function<NodeEvent, EventResult>): Boolean
}
