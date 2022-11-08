package com.tezrok.api.tree

import java.util.stream.Stream

/**
 * Storage of nodes
 */
interface NodeRepository {
    /**
     * Saves node and all it's children
     *
     * @return count of actually saved nodes
     */
    fun save(node: Node): Int

    /**
     * Returns root node
     */
    fun getRoot(): Node

    /**
     * Returns lazy list of nodes by search string
     */
    fun findNodes(term: String): Stream<Node>

    /**
     * Flushes cached data into storage
     */
    fun flush()
}
