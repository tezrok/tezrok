package com.tezrok.api.tree

import java.util.*
import java.util.stream.Stream

class NodeIml(
    private val id: Long,
    private val parentNode: Node?,
    private val nodeSupport: NodeSupport
) : Node {
    private val properties: Lazy<NodeProperties> = lazy { nodeSupport.getProperties(this) }

    override fun getId(): Long = id

    override fun getName(): String = properties.value.getStringProp(PropertyName.Name)

    override fun getType(): NodeType = NodeType.getOrCreate(properties.value.getStringProp(PropertyName.Type))

    override fun getParent(): Node? = parentNode

    override fun getRef(): NodeRef = NodeRefImpl(calcPath()) { path ->
        nodeSupport.findByPath(path)
    }

    override fun add(info: NodeInfo): Node = nodeSupport.add(this, info)

    override fun remove(nodes: List<Node>): Boolean = nodeSupport.remove(this, nodes)

    override fun getChildren(): Stream<Node> = nodeSupport.getChildren(this)

    override fun getChildrenSize(): Int = nodeSupport.getChildrenSize(this)

    override fun getProperties(): NodeProperties = properties.value

    override fun clone(): Node = nodeSupport.clone(this)

    /**
     * Calculating path for current node
     */
    private fun calcPath(): String {
        val nodes = LinkedList<Node>()
        var parent: Node? = this
        var size = 0;

        while (parent != null) {
            size += parent.getName().length
            nodes.addFirst(parent)
            parent = parent.getParent()
        }

        val sb = StringBuilder(size + nodes.size - 1)
        for (node in nodes) {
            if (sb.isNotEmpty()) {
                sb.append('/')
            }
            sb.append(node.getName())
        }

        return sb.toString()
    }

    override fun toString(): String = "${getType().name}: ${getName()}"
}
