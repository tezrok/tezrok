package com.tezrok.api.tree

import java.util.stream.Stream

/**
 * Entry point for accessing nodes
 */
interface NodeManager {
    /**
     * Returns root node
     */
    fun getRootNode(): Node

    /**
     * Find first node by path
     */
    fun findNodeByPath(path: String): Node?

    /**
     * Returns lazy list of nodes by search string
     */
    fun findNodes(term: String): Stream<Node>

    /**
     * Save all changes to the repository
     */
    fun save()
}
