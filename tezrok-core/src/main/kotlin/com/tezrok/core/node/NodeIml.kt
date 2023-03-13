package com.tezrok.core.node

import com.tezrok.api.error.NodeAlreadyExistsException
import com.tezrok.api.error.TezrokException
import com.tezrok.api.event.EventType
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.event.ResultType
import com.tezrok.api.node.*
import com.tezrok.api.service.NodeService
import com.tezrok.core.util.*
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.stream.Stream

/**
 * Basic implementation of the [Node]
 */
internal class NodeIml(
    private val id: Long,
    private val type: NodeType,
    private val parentNode: Node?,
    private val properties: NodeProperties,
    private val nodeSupport: NodeSupport
) : Node {
    // TODO: think about serialization
    internal var service: NodeService? = null
    private val children: Lazy<MutableList<Node>> = lazy { nodeSupport.loadChildren(this) }

    override fun getId(): Long = id

    override fun getName(): String = properties.getStringProperty(PropertyName.Name, null)

    override fun getType(): NodeType = type

    override fun getPath(): String = this.calcPath()

    override fun getParent(): Node? = parentNode

    override fun getRef(): NodeRef = NodeRefImpl(getPath()) { path -> nodeSupport.findNodeByPath(path) }

    override fun add(name: String, type: NodeType): Node =
        nodeSupport.writeLock {
            if (!canAddDuplicateNode()) {
                getChild(name)?.let { node ->
                    throw NodeAlreadyExistsException(name, node.getPath())
                }
            }
            val (author, authorType) = nodeSupport.getOperation()
            val eventResult = nodeSupport.onNodeEvent(NodeEvent(EventType.PreAdd, type, this, null))

            if (eventResult.type == ResultType.CANCEL) {
                val message = if (eventResult.message.isNullOrBlank()) "Node cannot be added" else eventResult.message
                throw TezrokException(message)
            }
            val newNode = nodeSupport.createNode(this, NodeElem.of(name, type))
            newNode.author(author)
            newNode.authorType(authorType)
            val now = OffsetDateTime.now()
            newNode.createdAt(now)
            newNode.updatedAt(now)
            // TODO: validate new node
            children.value.add(newNode)
            nodeSupport.onNodeEvent(NodeEvent(EventType.PostAdd, type, this, newNode))

            newNode
        }

    override fun remove(nodes: List<Node>): Boolean =
        nodeSupport.writeLock {
            val (author, authorType) = nodeSupport.getOperation()
            log.debug("{}:{} - Removing nodes: {}", authorType, author, nodes.map { it.getName() })
            children.value.removeAll(nodes)
        }

    override fun getChildren(): Stream<Node> {
        if (!children.isInitialized()) {
            children.value
        }

        return nodeSupport.readLock {
            children.value.toList().stream()
        }
    }

    override fun getOtherChildren(): Stream<Node> {
        TODO("Not yet implemented")
    }

    override fun getChildrenSize(): Int = children.value.size

    override fun findNodeByPath(path: String): Node? = nodeSupport.findNodeByPath(this, path)

    override fun getProperties(): NodeProperties = properties

    override fun <T : NodeService> asService(): T? {
        TODO("Not yet implemented")
    }

    override fun <T : Node> asChild(clazz: Class<T>): T? {
        TODO("Not yet implemented")
    }

    override fun clone(): Node = TODO("Not yet implemented")

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

    private companion object {
        private val log = LoggerFactory.getLogger(NodeIml::class.java)
    }
}
