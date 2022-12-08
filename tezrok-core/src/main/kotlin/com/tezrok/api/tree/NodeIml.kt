package com.tezrok.api.tree

import com.tezrok.util.calcPath
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

    override fun getPath(): String = this.calcPath()

    override fun getParent(): Node? = parentNode

    override fun getRef(): NodeRef = nodeSupport.getNodeRef(this)

    override fun add(name: String, type: NodeType): Node = nodeSupport.add(this, name, type)

    override fun remove(nodes: List<Node>): Boolean = nodeSupport.remove(this, nodes)

    override fun getChildren(): Stream<Node> = nodeSupport.getChildren(this)

    override fun getChildrenSize(): Int = nodeSupport.getChildrenSize(this)

    override fun findNodeByPath(path: String): Node? = nodeSupport.findNodeByPath(this, path)

    override fun getProperties(): NodeProperties = properties.value

    override fun clone(): Node = nodeSupport.clone(this)

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


