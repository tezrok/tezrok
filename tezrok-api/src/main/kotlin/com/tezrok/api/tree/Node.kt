package com.tezrok.api.tree

import java.util.stream.Stream

/**
 * Basic node interface
 *
 * TODO: manage files
 */
interface Node : Cloneable {
    /**
     * Returns unique identifier
     */
    fun getId(): Long

    /**
     * Returns name of the node
     */
    fun getName(): String

    /**
     * Returns type of the node
     */
    fun getType(): NodeType

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
     * Note: children can be of infinite size. Use nodesSize before.
     */
    fun getChildren(): Stream<Node>

    /**
     * Returns count of children
     *
     * @return Int.MAX_VALUE for infinite data
     */
    fun getChildrenSize(): Int

    /**
     * Updates property
     *
     * @return previous property value
     */
    fun setProperty(name: NodeProperty, value: Any?): Any?

    /**
     * Returns property value or null if property not set
     */
    fun getProperty(name: NodeProperty): Any?

    /**
     * Returns names of all properties. Can return names which not set yet (properties schema)
     */
    fun getPropertiesNames(): List<NodeProperty>

    /**
     * Return attributes of the node
     */
    fun getAttributes(): NodeAttributes

    /**
     * Return clone of the node
     */
    override fun clone(): Node
}
