package com.tezrok.api.tree

import org.apache.commons.lang3.Validate
import java.util.*
import java.util.stream.Stream

class NodeIml(
    private val id: Long,
    private val parentNode: Node?,
    private val properties: Map<String, Any?>,
    private val nodeSupport: NodeSupport
) : Node {
    override fun getId(): Long = id

    override fun getName(): String = getStringProp(NodeProperty.Id)

    override fun getType(): NodeType = NodeType.getOrCreate(getStringProp(NodeProperty.Type))

    override fun getParent(): Node? = parentNode

    override fun getRef(): NodeRef = NodeRefImpl(calcPath()) { path ->
        nodeSupport.findByPath(path)
    }

    override fun add(info: NodeInfo): Node {
        TODO("Not yet implemented")
    }

    override fun remove(nodes: List<Node>): Int {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Stream<Node> {
        TODO("Not yet implemented")
    }

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

    private fun getStringPropSafe(name: String): String? = properties[name] as String?

    private fun getStringProp(name: String): String =
        Validate.notBlank(getStringPropSafe(name), "Property '%s' cannot be empty", name)!!

    private fun getStringProp(property: NodeProperty): String = getStringProp(property.name)

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