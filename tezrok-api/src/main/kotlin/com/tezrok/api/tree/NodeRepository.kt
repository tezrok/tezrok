package com.tezrok.api.tree

import java.util.stream.Stream

/**
 * Storage of nodes
 */
interface NodeRepository {
    /**
     * Returns root node
     */
    fun getRoot(): NodeElem

    /**
     * Returns all children by parent Id
     */
    fun getChildren(parentId: Long): Stream<NodeElem>

    /**
     * Update single node by parentId
     */
    fun put(parentId: Long, node: NodeElem)

    /**
     * Flushes cached data into real storage
     */
    fun flush()
}
