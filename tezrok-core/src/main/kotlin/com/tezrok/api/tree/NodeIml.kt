package com.tezrok.api.tree

import com.tezrok.util.calcPath
import java.util.stream.Stream

/**
 * Basic implementation of the [Node]
 */
class NodeIml(
    private val id: Long,
    private val type: NodeType,
    private val parentNode: Node?,
    private val properties: NodeProperties,
    private val nodeSupport: NodeSupport
) : Node {
    override fun getId(): Long = id

    override fun getName(): String = properties.getStringProp(PropertyName.Name)

    override fun getType(): NodeType = type

    override fun getPath(): String = this.calcPath()

    override fun getParent(): Node? = parentNode

    override fun getRef(): NodeRef = nodeSupport.getNodeRef(this)

    override fun add(name: String, type: NodeType): Node = nodeSupport.add(this, name, type)

    override fun remove(nodes: List<Node>): Boolean = nodeSupport.remove(this, nodes)

    override fun getChildren(): Stream<Node> = nodeSupport.getChildren(this)

    override fun getChildrenSize(): Int = nodeSupport.getChildrenSize(this)

    override fun findNodeByPath(path: String): Node? = nodeSupport.findNodeByPath(this, path)

    override fun getProperties(): NodeProperties = properties

    override fun clone(): Node = nodeSupport.clone(this)

    override fun toString(): String = "Node-${getType().name}: ${getName()}"

    override fun hashCode(): Int {
        return 31 * id.hashCode() + (parentNode?.hashCode() ?: 0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Node) return false

        if (id != other.getId()) return false
        if (parentNode != other.getParent()) return false

        return true
    }
}
