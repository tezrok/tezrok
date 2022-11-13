package com.tezrok.api.tree

import com.tezrok.api.FileRef
import com.tezrok.api.error.TezrokException
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
     * Updates property
     *
     * @return previous property value
     */
    fun setProperty(name: NodeProperty, value: Any?): Any?

    /**
     * Returns property value or null if property not exists
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
     * Returns FileRef if the node is File node
     */
    fun getFile(): FileRef = getProperty(NodeProperty.File) as? FileRef ?: throw TezrokException("Node is not File")

    /**
     * Return clone of the node
     */
    override fun clone(): Node
}
