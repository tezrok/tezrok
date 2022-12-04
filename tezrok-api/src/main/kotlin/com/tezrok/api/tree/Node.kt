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
     * By default, is empty. Can be used by plugin as unique id
     * for node which not changes if the name changing
     */
    fun getUName(): String = ""

    /**
     * Returns parent
     *
     * Only root node returns null
     */
    fun getParent(): Node?

    /**
     * Returns reference of the node
     */
    fun getRef(): NodeRef

    /**
     * Adds new node or throws exception
     */
    fun add(info: NodeElem): Node

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
