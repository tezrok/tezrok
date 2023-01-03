package com.tezrok.feature

import com.tezrok.api.feature.FeatureNodeSupport
import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeProperties
import java.util.*
import java.util.stream.Stream

/**
 * Base implementation of [FeatureNodeSupport]
 *
 * @see FeatureNodeSupport
 */
open class FeatureNodeSupportImpl(
    private val id: Long,
    private val nodeProperties: NodeProperties,
    private val supportChildren: Boolean
) : FeatureNodeSupport {
    private val children: MutableList<Node> = Collections.synchronizedList(mutableListOf())

    override fun getId(): Long = id

    override fun getProperties(): NodeProperties = nodeProperties

    override fun isSupportChildren(): Boolean = supportChildren

    override fun getChildrenSize(): Int = children.size

    override fun getChildren(): Stream<Node> = children.toList().stream()

    override fun addNode(child: Node): Boolean {
        // TODO: check if child is supported
        children.add(child)
        return true
    }

    override fun removeNodes(nodes: List<Node>): Boolean {
        return children.removeAll(nodes)
    }
}
