package com.tezrok.api.tree

import com.tezrok.api.feature.FeatureNodeChildrenSupport
import com.tezrok.api.feature.FeatureNodeSelf
import com.tezrok.api.feature.Feature

/**
 * [Node] implementation for [Feature] node
 */
internal class FeatureNodeImpl(
    type: NodeType,
    parentNode: Node,
    val featureNode: Node,
    nodeSupport: NodeSupport
) : NodeIml(featureNode.getId(), type, parentNode, featureNode.getProperties(), nodeSupport) {
    init {
        if (featureNode is FeatureNodeSelf) {
            featureNode.setSelf(this)
        }
    }

    fun isSupportChildren(): Boolean = featureNode is FeatureNodeChildrenSupport
}

/**
 * Returns true if node is internal implementation
 */
internal fun Node.isInternalNode(): Boolean = this::class.java == NodeIml::class.java
        || this::class.java == FeatureNodeImpl::class.java
