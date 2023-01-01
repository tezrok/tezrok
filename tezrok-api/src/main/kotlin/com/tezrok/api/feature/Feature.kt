package com.tezrok.api.feature

import com.tezrok.api.tree.NodeType

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
     * Sets [InternalFeatureSupport] for the feature
     */
    fun setInternalFeatureSupport(internalFeatureSupport: InternalFeatureSupport)
}
