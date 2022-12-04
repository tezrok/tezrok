package com.tezrok.api.tree

/**
 * Class used while creating and in NodeRepository
 */
class NodeElem(
    val id: Long,

    val name: String,

    val type: NodeType,

    val properties: Map<PropertyName, Any?> = emptyMap()
)
