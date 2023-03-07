package com.tezrok.core.node

import com.tezrok.api.node.Node
import com.tezrok.api.node.NodeProperties
import com.tezrok.api.node.NodeRef
import com.tezrok.api.node.NodeType
import com.tezrok.api.service.NodeService
import com.tezrok.core.util.calcPath
import java.util.stream.Stream

internal abstract class AbstractNode(
    private val nodeId: NodeId,
    private val parent: Node?,
    private val properties: NodeProperties,
    private val nodeSupport: NodeSupport
) : Node {
    override fun getId(): Long = nodeId.id

    override fun getName(): String = nodeId.name

    override fun getType(): NodeType = nodeId.type

    override fun getPath(): String = this.calcPath()

    override fun getParent(): Node? = parent

    override fun getRef(): NodeRef = NodeRefImpl(getPath()) { path -> nodeSupport.findNodeByPath(path) }

    override fun add(name: String, type: NodeType): Node {
        TODO("Not yet implemented")
    }

    override fun remove(nodes: List<Node>): Boolean {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Stream<Node> {
        TODO("Not yet implemented")
    }

    override fun getOtherChildren(): Stream<Node> {
        TODO("Not yet implemented")
    }

    override fun getChildrenSize(): Int {
        TODO("Not yet implemented")
    }

    override fun findNodeByPath(path: String): Node? = nodeSupport.findNodeByPath(this, path)

    override fun getProperties(): NodeProperties = properties

    override fun <T : NodeService> asService(): T? {
        TODO("Not yet implemented")
    }

    override fun <T : Node> asChild(clazz: Class<T>): T? {
        TODO("Not yet implemented")
    }
}
