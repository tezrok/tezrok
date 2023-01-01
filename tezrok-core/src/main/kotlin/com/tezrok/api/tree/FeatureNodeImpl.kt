package com.tezrok.api.tree

import com.tezrok.api.feature.FeatureNodeSupport

class FeatureNodeImpl(
    type: NodeType,
    parentNode: Node,
    val featureSupport: FeatureNodeSupport,
    nodeSupport: NodeSupport
) : NodeIml(featureSupport.getId(), type, parentNode, featureSupport.getProperties(), nodeSupport) {
}
