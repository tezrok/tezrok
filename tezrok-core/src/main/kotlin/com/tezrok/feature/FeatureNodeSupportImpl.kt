package com.tezrok.feature

import com.tezrok.api.feature.FeatureNodeChildrenSupport
import com.tezrok.api.feature.InternalFeatureSupport
import com.tezrok.api.tree.*
import java.util.*
import java.util.stream.Stream

/**
 * Base implementation of [Feature] node
 *
 * @see FeatureNodeChildrenSupport
 */
open class FeatureNodeSupportImpl(
    private val id: Long,
    private val parent: Node,
    private val nodeProperties: NodeProperties,
    private val internalFeatureSupport: InternalFeatureSupport
) : Node, FeatureNodeChildrenSupport {
    private val children: MutableList<Node> = Collections.synchronizedList(mutableListOf())
    private lateinit var self: Node

    override fun getId(): Long = id

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getType(): NodeType {
        TODO("Not yet implemented")
    }

    override fun getPath(): String {
        TODO("Not yet implemented")
    }

    override fun getParent(): Node? = parent

    override fun getRef(): NodeRef {
        TODO("Not yet implemented")
    }

    override fun add(name: String, type: NodeType): Node {
        return internalFeatureSupport.createNode(self, NodeElem.of(name, type))
    }

    override fun remove(nodes: List<Node>): Boolean = children.removeAll(nodes)

    override fun getChildren(): Stream<Node> = children.toList().stream()

    override fun getChildrenSize(): Int = children.size

    override fun findNodeByPath(path: String): Node? {
        TODO("Not yet implemented")
    }

    override fun getProperties(): NodeProperties = nodeProperties

    override fun clone(): Node {
        TODO("Not yet implemented")
    }

    override fun setSelf(node: Node) {
        TODO("Not yet implemented")
    }
}
