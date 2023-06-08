package io.tezrok.api.node

import java.util.function.Predicate

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

    fun getChildren(): List<Node>

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
     *
     * Target name not included in path
     */
    fun getPathTo(target: Node?): String {
        val parents = ArrayDeque<Node>()
        var parent: Node? = this
        while (parent != null && parent != target) {
            parents.addFirst(parent)
            parent = parent.getParent()
        }

        if (target != null && parent != target) {
            throw IllegalArgumentException("Target node is not found: $target")
        }

        return parents.joinToString("/", prefix = "/") { it.getName() }
    }

    /**
     * Returns the first parent node that matches the predicate or null if not found
     */
    fun getFirstAncestor(predicate: Predicate<Node>): Node? {
        var parent: Node? = this.getParent()
        while (parent != null && !predicate.test(parent)) {
            parent = parent.getParent()
        }
        return parent
    }
}
