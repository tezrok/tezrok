package com.tezrok.api.tree

/**
 * Class used while creating and in NodeRepository
 */
class NodeElem(
    val id: Long,

    val properties: Map<PropertyName, String?> = emptyMap()
)
