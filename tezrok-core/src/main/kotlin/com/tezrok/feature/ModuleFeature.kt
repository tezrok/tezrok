package com.tezrok.feature

import com.tezrok.api.feature.CreateNodeFeature
import com.tezrok.api.feature.Feature
import com.tezrok.api.feature.FeatureNodeSupport
import com.tezrok.api.feature.InternalFeatureSupport
import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeProperties
import com.tezrok.api.tree.NodeType
import com.tezrok.api.tree.getNodeType

/**
 * Implementation of [Feature] for [NodeType.Module]
 *
 * @see Feature
 * @since 1.0
 */
class ModuleFeature : CreateNodeFeature {
    private lateinit var internalFeatureSupport: InternalFeatureSupport

    override fun getName(): String = "CoreModuleFeature"

    override fun getDescription(): String = "Implementation of Feature for Module"

    override fun getNodeTypes(): List<NodeType> = listOf(NodeType.Module)

    override fun setInternalFeatureSupport(internalFeatureSupport: InternalFeatureSupport) {
        this.internalFeatureSupport = internalFeatureSupport
    }

    override fun createNode(parent: Node, properties: NodeProperties, id: Long): FeatureNodeSupport? {
        if (properties.getNodeType() != NodeType.Module) {
            return null
        }

        val finalId = if (id > 0) id else internalFeatureSupport.getNextNodeId()

        return FeatureNodeSupportImpl(finalId, properties, false)
    }
}
