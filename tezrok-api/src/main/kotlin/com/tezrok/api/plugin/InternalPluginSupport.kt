package com.tezrok.api.plugin

import com.tezrok.api.event.EventResult
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.node.Node
import com.tezrok.api.node.NodeType
import com.tezrok.api.service.TezrokService
import java.util.function.Function

/**
 * Used by a plugin to access internal functionality
 */
interface InternalPluginSupport {
    /**
     * Returns next unique id for the node
     */
    fun getNextNodeId(): Long

    /**
     * Add subscriber to the event
     */
    fun subscribeOnEvent(type: NodeType, handler: Function<NodeEvent, EventResult>)

    /**
     * Remove subscriber
     */
    fun unsubscribeOnEvent(handler: Function<NodeEvent, EventResult>): Boolean

    /**
     * Set service for the node
     */
    fun setService(node: Node, service: TezrokService): Boolean
}
