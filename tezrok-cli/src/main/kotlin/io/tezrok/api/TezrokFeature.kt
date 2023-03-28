package io.tezrok.api

import io.tezrok.api.node.Node

/**
 * Represents a feature which can be applied to a project
 */
interface TezrokFeature {
    fun apply(node: Node, context: GeneratorContext)
}
