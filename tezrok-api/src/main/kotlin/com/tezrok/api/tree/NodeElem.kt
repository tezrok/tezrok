package com.tezrok.api.tree

/**
 * Simple data representation of a [Node]
 */
data class NodeElem(
    /**
     * Unique node id
     */
    val id: Long,

    /**
     * All properties except [PropertyName.Id]
     */
    val properties: Map<PropertyName, String?> = emptyMap()
) {
    companion object {
        @JvmStatic
        fun of(name: String, type: NodeType): NodeElem {
            return NodeElem(
                id = 0,
                properties = mapOf(
                    PropertyName.Name to name,
                    PropertyName.Type to type.name
                )
            )
        }
    }
}
