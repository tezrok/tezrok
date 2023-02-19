package com.tezrok.core.node.repo.file

import com.tezrok.api.node.NodeElem
import com.tezrok.api.node.NodeType
import com.tezrok.api.node.PropertyName

/**
 * Dto for file repository purposes
 */
internal data class FileNodeElem(
    val id: Long,

    val props: Map<String, String?>? = null,

    val items: List<FileNodeElem>? = null
) {
    fun toElem(): NodeElem = NodeElem(
        id,
        properties = props?.map { PropertyName.getOrCreate(it.key) to it.value }?.toMap() ?: emptyMap()
    )

    companion object {
        const val ROOT_ID = 1000L

        const val ROOT_NAME = "Root"

        fun of(id: Long, name: String, type: NodeType): FileNodeElem =
            FileNodeElem(
                id = id,
                props = mapOf(PropertyName.Name.name to name, PropertyName.Type.name to type.name)
            )

        /**
         * Default root element
         */
        val ROOT = of(ROOT_ID, ROOT_NAME, NodeType.Root)
    }
}
