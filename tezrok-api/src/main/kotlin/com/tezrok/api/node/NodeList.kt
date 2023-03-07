package com.tezrok.api.node

/**
 * Interface interpreted as a list of nodes
 */
interface NodeList<out T> : List<T> where T : Node {
    fun create(name: String): T

    fun add(node: @UnsafeVariance T)

    fun remove(node: List<@UnsafeVariance T>): Boolean
}
