package com.tezrok.api.plugin

import com.tezrok.api.event.EventResult
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeType
import java.util.function.Function

/**
 * Used by a Plugin's [Feature] to access internal [Node] functionality
 */
interface InternalPluginSupport {
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
