package com.tezrok.api.tree.repo.file

import com.tezrok.api.tree.NodeElem
import com.tezrok.api.tree.NodeType
import com.tezrok.api.tree.PropertyName

data class FileNodeElem(
    val id: Long,

    val name: String,

    val type: String,

    val properties: Map<String, Any?>? = null,

    val items: List<FileNodeElem>? = null
) {
    fun toElem(): NodeElem = NodeElem(
        id, name = name,
        type = NodeType.getOrCreate(type),
        properties = properties?.map { PropertyName.getOrCreate(it.key) to it.value }?.toMap() ?: emptyMap()
    )

    companion object {
        const val ROOT_ID = 1000L

        const val ROOT_NAME = "Root"

        /**
         * Default root element
         */
        val ROOT = FileNodeElem(ROOT_ID, ROOT_NAME, NodeType.Root.name)
    }
}
