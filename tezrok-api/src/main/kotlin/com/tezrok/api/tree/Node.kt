package com.tezrok.api.tree

import java.util.stream.Stream

/**
 * Basic node interface
 */
interface Node {
    /**
     * Returns unique identifier
     */
    fun getId(): Long

    /**
     * Returns name of the node
     */
    fun getName(): String

    /**
     * Adds new node or throws exception
     */
    fun add(info: NodeInfo): Node

    /**
     * Remove specified nodes
     *
     * @return returns count of removed nodes
     */
    fun remove(nodes: List<Node>): Int

    /**
     * Return children nodes
     *
     * Note: children can be of infinitive size. Use nodesSize before.
     */
    fun getChildren(): Stream<Node>

    /**
     * Returns count of children
     *
     * @return Int.MAX_VALUE for infinitive data
     */
    fun getChildrenSize(): Int

    /**
     * Updates property
     *
     * @return previous proverty value
     */
    fun setProperty(name: String, value: Any?): Any?

    /**
     * Returns property value or null if property not set
     */
    fun getProperty(name: String): Any?
}
