package com.tezrok.api.node

/**
 * Interface interpreted as a list of nodes
 */
interface NodeList<out T> : List<T> where T : Node {
    /**
     * Create a new node
     */
    fun create(name: String): T

    /**
     * Add a node
     */
    fun add(node: @UnsafeVariance T)

    /**
     * Remove specified nodes
     *
     * @return true if at least one node was removed
     */
    fun remove(nodes: List<@UnsafeVariance T>): Boolean
}
