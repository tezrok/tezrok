package com.tezrok.core.service

import com.tezrok.api.feature.Feature
import com.tezrok.api.feature.FeatureService
import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeType
import com.tezrok.core.feature.ModuleFeature
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * [FeatureService] implementation for [Node] of all system types ([NodeType])
 *
 * @see FeatureService
 * @see NodeType
 * @since 1.0
 */
internal class CoreFeatureService : FeatureService {
    private val moduleFeature: Lazy<ModuleFeature> = lazy { ModuleFeature() }

    init {
        log.info("CoreFeatureService initialized")
    }

    override fun getSupportedNodeTypes(): List<NodeType> = listOf(NodeType.Module)

    override fun getFeatures(nodeType: NodeType): List<Feature> =
        when (nodeType) {
            NodeType.Module -> listOf(moduleFeature.value)
            else -> emptyList()
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(CoreFeatureService::class.java)
    }
}
