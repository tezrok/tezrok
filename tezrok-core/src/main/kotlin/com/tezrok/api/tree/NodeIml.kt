package com.tezrok.api.tree

import org.apache.commons.lang3.Validate
import java.util.*
import java.util.stream.Stream

class NodeIml(
    private val id: Long,
    private val parentNode: Node?,
    private val properties: Map<NodeProperty, Any?>,
    private val nodeSupport: NodeSupport
) : Node {
    override fun getId(): Long = id

    override fun getName(): String = getStringProp(NodeProperty.Name)

    override fun getType(): NodeType = NodeType.getOrCreate(getStringProp(NodeProperty.Type))

    override fun getParent(): Node? = parentNode

    override fun getRef(): NodeRef = NodeRefImpl(calcPath()) { path ->
        nodeSupport.findByPath(path)
    }

    override fun add(info: NodeInfo): Node = nodeSupport.add(this, info )

    override fun remove(nodes: List<Node>): Boolean = nodeSupport.remove(this, nodes)

    override fun getChildren(): Stream<Node> = nodeSupport.getChildren(this)

    override fun getChildrenSize(): Int {
        TODO("Not yet implemented")
    }

    override fun setProperty(name: NodeProperty, value: Any?): Any? {
        TODO("Not yet implemented")
    }

    override fun getProperty(name: NodeProperty): Any? {
        TODO("Not yet implemented")
    }

    override fun getPropertiesNames(): List<NodeProperty> {
        TODO("Not yet implemented")
    }

    override fun getAttributes(): NodeAttributes {
        TODO("Not yet implemented")
    }

    override fun clone(): Node {
        TODO("Not yet implemented")
    }

    private fun getStringPropSafe(name: NodeProperty): String? = properties[name] as String?

    private fun getStringProp(name: NodeProperty): String =
        Validate.notBlank(getStringPropSafe(name), "Property '%s' cannot be empty", name)!!

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
}