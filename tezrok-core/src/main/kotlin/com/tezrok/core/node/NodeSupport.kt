package com.tezrok.core.node

import com.tezrok.api.error.TezrokException
import com.tezrok.api.event.EventResult
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.node.*
import com.tezrok.api.plugin.TezrokPlugin
import com.tezrok.api.service.NodeService
import com.tezrok.api.service.TezrokService
import com.tezrok.core.feature.FeatureManager
import com.tezrok.core.util.*
import org.apache.commons.lang3.Validate
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Main implementation of the [Node]
 */
internal class NodeSupport(
    private val nodeRepo: NodeRepository,
    private val featureManager: FeatureManager,
    private val propertyValueManager: PropertyValueManager
) {
    private val lastIdCounter = AtomicLong(nodeRepo.getLastId())
    private val lock = ReentrantReadWriteLock()
    private val writeLock = lock.writeLock()
    private val readLock = lock.readLock()
    private val root = lazy { createRoot() }
    private val operations = ArrayDeque<Pair<String, AuthorType>>()

    init {
        log.info("NodeSupport initialized")
        featureManager.setNodeSupport(this)
        propertyValueManager.nodeSupport = this
    }

    /**
     * Find first node by path
     */
    fun findNodeByPath(path: String): Node? = findNodeByPath(root.value, path)

    /**
     * Find first child node by path
     */
    fun findNodeByPath(node: Node, path: String): Node? {
        if (path.isBlank() || path[0] != '/') {
            return null
        }

        if (path == "/") {
            return node
        }

        val index = path.indexOf('/', 1)
        val name = if (index == -1) path.substring(1) else path.substring(1, index)
        if (name.isBlank()) {
            return null
        }
        val child = node.getChild(name) ?: return null

        return if (index == -1) child else findNodeByPath(child, path.substring(index))
    }

    fun getRoot(): Node = root.value

    fun onNodeEvent(event: NodeEvent): EventResult = featureManager.onNodeEvent(event)

    fun <R> writeLock(block: () -> R): R = writeLock.runIn(block)

    fun <R> readLock(block: () -> R): R = readLock.runIn(block)

    private fun createRoot(): Node {
        val rootElem = nodeRepo.getRoot()

        writeLock.runIn {
            val nodeProps = NodePropertiesImpl(rootElem.properties, propertyValueManager)
            val node = NodeIml(rootElem.id, NodeType.Root, null, nodeProps, this)
            nodeProps.setNode(node)
            node.author(AuthorTypeName.System)
            node.authorType(AuthorType.System)
            val now = OffsetDateTime.now()
            node.createdAt(now)
            node.updatedAt(now)
            return node
        }
    }

    /**
     * Load children from repository and add to cache
     */
    internal fun loadChildren(parent: Node): MutableList<Node> =
        writeLock.runIn {
            nodeRepo.getChildren(parent.getId())
                .map { elem -> createNode(parent, elem) }
                .collect(Collectors.toList()) // don't use Stream.toList() because it's immutable
        }

    fun getNextNodeId(): Long = lastIdCounter.incrementAndGet()

    fun subscribeOnEvent(plugin: TezrokPlugin, type: NodeType, handler: Function<NodeEvent, EventResult>) =
        featureManager.subscribeOnEvent(plugin, type, handler)

    fun unsubscribeOnEvent(handler: Function<NodeEvent, EventResult>): Boolean =
        featureManager.unsubscribeOnEvent(handler)

    fun startOperation(type: AuthorType, author: String): NodeOperation {
        val authorType = author to type
        writeLock.runIn {
            operations.add(authorType)
        }

        log.info("Started operation by {}: {}", type, author)

        return object : NodeOperation {
            override val author: String
                get() = author
            override val type: AuthorType
                get() = type

            override fun stop() {
                writeLock.runIn {
                    Validate.isTrue(operations.isNotEmpty(), "Operation queue is empty")
                    Validate.isTrue(
                        operations.removeLast() == authorType,
                        "Operation is not started by %s", author
                    )
                }

                log.info("Stopped operation by {}: {}", authorType, author)
            }
        }
    }

    fun getOperation(): Pair<String, AuthorType> =
        operations.lastOrNull() ?: throw TezrokException("Node operation not started")

    fun createNode(parent: Node, nodeElem: NodeElem): Node {
        val nodeProps = NodePropertiesImpl(nodeElem.properties, propertyValueManager)
        val nodeType = nodeProps.getNodeType()
        val id = if (nodeElem.id > 0) nodeElem.id else getNextNodeId()
        val newNode = NodeIml(id, nodeType, parent, nodeProps, this@NodeSupport)
        nodeProps.setNode(newNode)
        return newNode
    }

    fun setService(node: Node, service: TezrokService): Boolean {
        if (node is NodeIml && service is NodeService) {
            node.service = service
            return true
        }
        log.warn("Service {} cannot be set to node {}", service.javaClass.name, node)
        return false
    }


    private companion object {
        private val log = LoggerFactory.getLogger(NodeSupport::class.java)
    }
}

internal fun Node.canAddDuplicateNode(): Boolean =
    getProperties().getBooleanProperty(PropertyName.DuplicateNode, false)

/**
 * Author's type of node operation
 */
internal enum class AuthorType {
    System,
    User,
    Plugin
}
