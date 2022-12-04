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

    override fun add(info: NodeElem): Node = nodeSupport.add(this, info)

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

    override fun toString(): String = "Node-${getType().name}: ${getName()}"

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (parentNode?.hashCode() ?: 0)
        result = 31 * result + properties.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Node) return false

        if (id != other.getId()) return false
        if (parentNode != other.getParent()) return false
        if (properties.value != other.getProperties()) return false

        return true
    }
}

private fun Node.toElem(): NodeElem =
    NodeElem(id = getId(), name = getName(), type = getType(), properties = toProps(getProperties()))

private fun toProps(properties: NodeProperties): Map<PropertyName, Any?> =
    properties.getPropertiesNames().associateWith { properties.getProperty(it) }
