package com.tezrok.api.feature

import com.tezrok.api.tree.Node

interface FeatureNodeSelf {
    /**
     * Sets real internal node
     */
    fun setSelf(node: Node)
}
