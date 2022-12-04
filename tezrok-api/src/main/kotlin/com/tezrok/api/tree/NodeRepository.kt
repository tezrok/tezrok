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
     *
     * @param parentId parent id
     */
    fun getChildren(parentId: Long): Stream<NodeElem>

    /**
     * Update single node by parentId
     *
     * @param parentId parent id or 0 for root node
     * @param node node to update
     */
    fun put(parentId: Long, node: NodeElem)

    /**
     * Flushes cached data into real storage
     */
    fun save()

    /**
     * Returns last used id
     */
    fun getLastId(): Long
}
