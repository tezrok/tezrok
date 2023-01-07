package com.tezrok.core.feature

import com.tezrok.api.event.EventResult
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.feature.Feature
import com.tezrok.api.feature.InternalFeatureSupport
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
        TODO("Not yet implemented")
    }

    override fun setInternalFeatureSupport(internalFeatureSupport: InternalFeatureSupport) {
        this.internalFeatureSupport = internalFeatureSupport
    }
}
