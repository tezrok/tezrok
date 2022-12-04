package com.tezrok.api.tree

class NodeElem(
    val id: Long,

    val name: String,

    val type: NodeType,

    val properties: Map<PropertyName, Any?> = emptyMap()
)
