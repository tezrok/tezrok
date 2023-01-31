package com.tezrok.api.feature

import com.tezrok.api.event.EventResult
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.node.NodeType

/**
 * Basic interface for all features
 */
interface Feature {
    /**
     * Returns name of the feature
     */
    fun getName(): String

    /**
     * Returns description of the feature
     */
    fun getDescription(): String

    /**
     * Returns supported node types
     */
    fun getNodeTypes(): List<NodeType>

    /**
     * Called when node event occurs
     */
    fun onNodeEvent(event: NodeEvent): EventResult
}
