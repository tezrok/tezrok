package com.tezrok.api.tree

import java.util.stream.Stream

/**
 * Basic node interface
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
     * Unique name related with functionality
     *
     * By default, equals to the name. But can be used by plugin as unique id
     * for node which not changes if the name changing
     */
    fun getFuncName(): String = getName()

    /**
     * Returns parent
     *
     * Root node returns null
     */
    fun getParent(): Node?

    /**
     * Returns reference of the node
     */
    fun getRef(): NodeRef

    /**
     * Adds new node or throws exception
     */
    fun add(info: NodeInfo): Node

    /**
     * Remove specified nodes
     *
     * @return `true` if any of the specified nodes was removed from the node
     */
    fun remove(nodes: List<Node>): Boolean

    /**
     * Return children nodes
     *
     * Note: children can be of infinite size. Call `getChildrenSize` before.
     */
    fun getChildren(): Stream<Node>

    /**
     * Returns count of children
     *
     * @return Int.MAX_VALUE for infinite data
     */
    fun getChildrenSize(): Int

    /**
     * Return properties of the node
     */
    fun getProperties(): NodeProperties

    /**
     * Return clone of the node
     */
    override fun clone(): Node
}
