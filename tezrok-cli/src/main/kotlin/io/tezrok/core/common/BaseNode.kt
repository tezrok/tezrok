package io.tezrok.core.common

import org.slf4j.LoggerFactory

/**
 * Common implementation for all nodes
 */
abstract class BaseNode(private var name: String, private val parent: BaseNode?) : Node {
    protected val log = LoggerFactory.getLogger(this.javaClass)!!

    /**
     * Returns the name of the node
     */
    override fun getName(): String = name

    /**
     * Sets the name of the node
     */
    override fun setName(name: String) {
        // TODO: validate name
        this.name = name
    }

    override fun getParent(): BaseNode? = parent
}

/**
 * Represents a node
 */
interface Node {
    /**
     * Returns the name of the node
     */
    fun getName(): String

    /**
     * Sets the name of the node
     */
    fun setName(name: String)

    /**
     * Returns the parent node
     *
     * null if this is the root node
     */
    fun getParent(): Node?

    /**
     * Returns true if this is the root node
     */
    fun isRoot(): Boolean = getParent() == null

    /**
     * Returns the logical path of the node
     *
     * Logical path based only on the name of the node
     * Parent name not included in path
     */
    fun getPath(): String = getPathTo(null)

    /**
     * Returns path up to the target node
     */
    fun getPathTo(target: Node?): String {
        // Parent name not included in path
        val parent = getParent() ?: return "/"
        val parentPath = if (parent.isRoot() || this == target) "" else parent.getPathTo(target)
        return parentPath + "/" + getName()
    }
}
