package com.tezrok.core.plugin

import com.tezrok.api.plugin.TezrokPlugin
import com.tezrok.api.event.EventResult
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.plugin.InternalPluginSupport
import com.tezrok.api.node.Node
import com.tezrok.api.node.NodeType
import com.tezrok.core.tree.NodeSupport
import java.util.function.Function

internal class PluginInternalPluginSupport(
    private val plugin: TezrokPlugin,
    private val support: NodeSupport
) : InternalPluginSupport {
    override fun getNextNodeId(): Long = support.getNextNodeId()

    override fun applyNode(node: Node): Boolean = support.applyNode(node)

    override fun subscribeOnEvent(type: NodeType, handler: Function<NodeEvent, EventResult>) =
        support.subscribeOnEvent(plugin, type, handler)

    override fun unsubscribeOnEvent(handler: Function<NodeEvent, EventResult>): Boolean =
        support.unsubscribeOnEvent(handler)
}
