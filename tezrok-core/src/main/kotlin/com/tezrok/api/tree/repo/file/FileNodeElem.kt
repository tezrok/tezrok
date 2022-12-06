package com.tezrok.api.tree.repo.file

import com.tezrok.api.tree.NodeElem
import com.tezrok.api.tree.NodeType
import com.tezrok.api.tree.PropertyName

/**
 * Dto for file repository purposes
 */
data class FileNodeElem(
    val id: Long,

    val properties: Map<String, Any?>? = null,

    val items: List<FileNodeElem>? = null
) {
    fun toElem(): NodeElem = NodeElem(
        id,
        properties = properties?.map { PropertyName.getOrCreate(it.key) to it.value }?.toMap() ?: emptyMap()
    )

    companion object {
        const val ROOT_ID = 1000L

        const val ROOT_NAME = "Root"

        fun of(id: Long, name: String, type: NodeType): FileNodeElem =
            FileNodeElem(
                id = id,
                properties = mapOf(PropertyName.Name.name to name, PropertyName.Type.name to type.name)
            )

        /**
         * Default root element
         */
        val ROOT = of(ROOT_ID, ROOT_NAME, NodeType.Root)
    }
}
