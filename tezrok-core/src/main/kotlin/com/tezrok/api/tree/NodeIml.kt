package com.tezrok.api.tree

import org.apache.commons.lang3.Validate
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

class NodeIml(
    private val id: Long,
    private val parentNode: Node?,
    private val props: Map<NodeProperty, Any?>,
    private val nodeSupport: NodeSupport
) : Node {
    private val properties: MutableMap<NodeProperty, Any?> = ConcurrentHashMap(props)

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

    override fun getChildrenSize(): Int = nodeSupport.getChildrenSize(this)

    override fun setProperty(property: NodeProperty, value: Any?): Any? {
        val oldProp = properties.put(property, value)
        // TODO: check input
        return oldProp
    }

    override fun getProperty(property: NodeProperty): Any? = properties[property]

    override fun getPropertiesNames(): Set<NodeProperty> = properties.keys

    override fun getAttributes(): NodeAttributes {
        TODO("Not yet implemented")
    }

    override fun clone(): Node = nodeSupport.clone(this)

    private fun getStringPropSafe(property: NodeProperty): String? = properties[property] as String?

    private fun getStringProp(property: NodeProperty): String =
        Validate.notBlank(getStringPropSafe(property), "Property '%s' cannot be empty", property)!!

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
}
