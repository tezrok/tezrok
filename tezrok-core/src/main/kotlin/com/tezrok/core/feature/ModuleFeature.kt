package com.tezrok.core.feature

import com.tezrok.api.event.EventResult
import com.tezrok.api.event.EventType
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.feature.Feature
import com.tezrok.api.feature.InternalFeatureSupport
import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeType

/**
 * Implementation of [Feature] for [NodeType.Module]
 *
 * @see Feature
 * @since 1.0
 */
internal class ModuleFeature : Feature {
    private lateinit var internalFeatureSupport: InternalFeatureSupport

    override fun getName(): String = "CoreModuleFeature"

    override fun getDescription(): String = "Implementation of Feature for Module"

    override fun getNodeTypes(): List<NodeType> = listOf(NodeType.Module)

    override fun onNodeEvent(event: NodeEvent): EventResult {
        if (event.eventType == EventType.PostAdd) {
            val node = event.node!!

            if (node.getType() == NodeType.Module) {
                addModuleFoldersIfNecessary(node)
            }
        }

        return EventResult.Continue
    }

    private fun addModuleFoldersIfNecessary(node: Node) {
        if (node.getChild("Types") == null) {
            node.add("Types", NodeType.Types)
        }
        if (node.getChild("Features") == null) {
            node.add("Services", NodeType.Services)
        }
    }

    override fun setInternalFeatureSupport(internalFeatureSupport: InternalFeatureSupport) {
        this.internalFeatureSupport = internalFeatureSupport
    }
}
