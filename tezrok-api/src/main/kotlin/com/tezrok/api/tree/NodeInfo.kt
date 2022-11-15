package com.tezrok.api.tree

data class NodeInfo(
    val name: String,

    val type: NodeType,

    val properties: Map<PropertyName, Any?> = emptyMap()
)
