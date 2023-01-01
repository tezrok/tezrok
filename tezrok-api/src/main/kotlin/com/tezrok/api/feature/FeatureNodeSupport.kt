package com.tezrok.api.feature

import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeProperties
import java.util.stream.Stream

/**
 * Interface for supporting [Node] functionality
 *
 * TODO: add more methods
 */
interface FeatureNodeSupport {
    fun getId(): Long

    fun getProperties(): NodeProperties

    /**
     * Returns true if node supports children itself
     */
    fun isSupportChildren(): Boolean

    /**
     * Returns children size of the node
     *
     * Used only if [isSupportChildren] returns true
     */
    fun getChildrenSize(): Int

    /**
     * Returns children of the node
     *
     * Used only if [isSupportChildren] returns true
     */
    fun getChildren(): Stream<Node>

    /**
     * Adds child [Node] to the node
     *
     * Used only if [isSupportChildren] returns true
     */
    fun addNode(child: Node): Boolean

    /**
     * Removes list of [Node]s from the node
     *
     * Used only if [isSupportChildren] returns true
     */
    fun removeNodes(nodes: List<Node>): Boolean
}
