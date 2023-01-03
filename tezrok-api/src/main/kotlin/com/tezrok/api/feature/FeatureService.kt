package com.tezrok.api.feature

import com.tezrok.api.TezrokService
import com.tezrok.api.tree.NodeType

/**
 * Entry point for all features
 */
interface FeatureService : TezrokService {
    /**
     * Returns all supported node types
     */
    fun getSupportedNodeTypes(): List<NodeType>

    /**
     * Returns feature by [NodeType]
     */
    fun getFeatures(nodeType: NodeType): List<Feature>
}
