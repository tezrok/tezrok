package com.tezrok.api.feature

import com.tezrok.api.tree.NodeProperties
import java.util.stream.Stream

/**
 * Interface for supporting [Node] functionality
 *
 * TODO: add more methods
 */
interface FeatureNodeSupport {
    fun getProperties(): NodeProperties

    fun getChildren(): Stream<FeatureNodeSupport>
}
