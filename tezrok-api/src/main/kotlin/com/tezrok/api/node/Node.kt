package com.tezrok.api.node

import com.tezrok.api.common.Named
import com.tezrok.api.error.NodeAlreadyExistsException
import com.tezrok.api.service.NodeService
import java.util.stream.Stream

/**
 * Basic node interface
 */
interface Node : Named, Cloneable {
    /**
     * Returns unique identifier
     */
    fun getId(): Long

    /**
     * Returns name of the node
     */
    override fun getName(): String

    /**
     * Returns type of the node
     */
    fun getType(): NodeType

    /**
     * Returns full path of the node
     */
    fun getPath(): String

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
     * Adds new node
     *
     * @throws NodeAlreadyExistsException if node with such name already exists and duplicates not allowed
     */
    fun add(name: String, type: NodeType): Node

    /**
     * Adds new node
     *
     * @throws NodeAlreadyExistsException if node with such name already exists and duplicates not allowed
     */
    fun add(name: String): Node = add(name, NodeType.Any)

    /**
     * Remove specified child nodes
     *
     * @return `true` if any of the specified nodes was removed from the node
     */
    fun remove(nodes: List<Node>): Boolean

    /**
     * Return all children nodes including of unknown types
     *
     * Note: children can be of infinite size. Call `getChildrenSize` before.
     */
    fun getChildren(): Stream<Node>

    /**
     * Returns children nodes except known types
     */
    fun getOtherChildren(): Stream<Node>

    /**
     * Returns count of children
     *
     * @return Int.MAX_VALUE for infinite data
     */
    fun getChildrenSize(): Int

    /**
     * Return child node by name. If not found, returns null
     */
    fun getChild(name: String): Node? = getChildren().filter { it.getName() == name }.findFirst().orElse(null)

    /**
     * Returns true if node has infinite children
     */
    fun isInfinite(): Boolean = getChildrenSize() == Int.MAX_VALUE

    /**
     * Returns first child by path. If not found, returns null
     *
     * By path "/" returns self
     *
     * @param path path to child
     */
    fun findNodeByPath(path: String): Node?

    /**
     * Return properties of the node
     */
    fun getProperties(): NodeProperties

    /**
     * Returns true if node is root
     */
    fun isRoot(): Boolean = getType() == NodeType.Root

    /**
     * Returns attached service
     */
    fun <T : NodeService> asService(): T?

    /**
     * Returns extended child node. Known child nodes can be got by this method as well
     *
     * Returns null if node is not supported
     */
    fun <T : Node> asChild(clazz: Class<T>): T?

    /**
     * Return clone of the node
     */
    override fun clone(): Node
}
