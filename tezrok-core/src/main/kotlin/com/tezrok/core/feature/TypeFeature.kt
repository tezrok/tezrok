package com.tezrok.core.feature

import com.tezrok.api.event.EventResult
import com.tezrok.api.event.EventType
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.feature.Feature
import com.tezrok.api.node.NodeType
import com.tezrok.api.plugin.InternalPluginSupport
import com.tezrok.api.service.TezrokService

internal class TypeFeature : Feature {
    internal lateinit var pluginSupport: InternalPluginSupport

    override fun getName(): String = "CoreTypeFeature"

    override fun getDescription(): String = "Implementation of Feature for Type"

    override fun getNodeTypes(): List<NodeType> = listOf(NodeType.Types)

    override fun onNodeEvent(event: NodeEvent): EventResult {
        if (event.eventType == EventType.PostAdd) {
            val node = event.node!!

            if (node.getType() == NodeType.Types) {
                // TODO: set implementation of TypeFolder
                pluginSupport.setService(node, TezrokService.Empty)
            }
        }

        return EventResult.Continue
    }
}