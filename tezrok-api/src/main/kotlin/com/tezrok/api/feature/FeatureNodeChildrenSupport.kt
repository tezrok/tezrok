package com.tezrok.api.feature

import com.tezrok.api.tree.Node

/**
 * Marker interface for [Node] children support
 *
 * If [Node] implements this interface, it means that it supports children itself
 *
 * @see Feature
 * @see Node
 */
interface FeatureNodeChildrenSupport : FeatureNodeSelf {
}
