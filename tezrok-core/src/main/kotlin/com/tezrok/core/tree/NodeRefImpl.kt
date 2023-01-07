package com.tezrok.core.tree

import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeRef

/**
 * Reference to the node
 */
internal class NodeRefImpl(
    private val path: String,
    private val handler: (String) -> Node?
) : NodeRef {
    override fun getPath(): String = path

    override fun getNode(): Node? = handler(path)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeRef) return false

        if (path != other.getPath()) return false

        return true
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    override fun toString(): String = "NodeRefImpl(path='$path')"
}
