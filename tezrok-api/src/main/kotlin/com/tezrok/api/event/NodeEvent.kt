package com.tezrok.api.event

import com.tezrok.api.node.Node
import com.tezrok.api.node.NodeType

/**
 * Base event class of [Node]
 *
 * @see EventType
 */
open class NodeEvent(
    val eventType: EventType,
    val type: NodeType,
    val parent: Node,
    val node: Node?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeEvent) return false

        if (eventType != other.eventType) return false
        if (type != other.type) return false
        if (parent != other.parent) return false
        if (node != other.node) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eventType.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + parent.hashCode()
        result = 31 * result + (node?.hashCode() ?: 0)
        return result
    }
}
