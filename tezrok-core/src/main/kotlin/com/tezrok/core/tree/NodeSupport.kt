package com.tezrok.core.tree

import com.tezrok.api.plugin.TezrokPlugin
import com.tezrok.api.error.NodeAlreadyExistsException
import com.tezrok.api.error.TezrokException
import com.tezrok.api.event.EventResult
import com.tezrok.api.event.EventType
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.event.ResultType
import com.tezrok.api.tree.*
import com.tezrok.core.feature.FeatureManager
import com.tezrok.core.util.*
import com.tezrok.core.util.author
import com.tezrok.core.util.authorType
import com.tezrok.core.util.calcPath
import org.apache.commons.lang3.Validate
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Main implementation of the [Node]
 */
internal class NodeSupport(
    private val nodeRepo: NodeRepository,
    private val featureManager: FeatureManager,
    private val propertyValueManager: PropertyValueManager
) {
    private val lastIdCounter = AtomicLong(nodeRepo.getLastId())
    private val nodes: MutableMap<Node, MutableList<Node>> = HashMap()
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

    fun add(parent: Node, name: String, type: NodeType): Node {
        writeLock.runIn {
            if (!parent.canAddDuplicateNode()) {
                parent.getChild(name)?.let { node ->
                    throw NodeAlreadyExistsException(name, node.getPath())
                }
            }
            val (author, authorType) = getOperation()
            val eventResult = featureManager.onNodeEvent(NodeEvent(EventType.PreAdd, type, parent, null))

            if (eventResult.type == ResultType.CANCEL) {
                val message = if (eventResult.message.isNullOrBlank()) "Node cannot be added" else eventResult.message
                throw TezrokException(message)
            }
            val newNode = createNode(parent, NodeElem.of(name, type))
            newNode.author(author)
            newNode.authorType(authorType)
            val now = OffsetDateTime.now()
            newNode.createdAt(now)
            newNode.updatedAt(now)
            // TODO: validate new node
            nodes.computeIfAbsent(parent) { ArrayList() }.add(newNode)
            featureManager.onNodeEvent(NodeEvent(EventType.PostAdd, type, parent, newNode))

            return newNode
        }
    }

    fun remove(parent: Node, nodes: List<Node>): Boolean {
        writeLock.runIn {
            val (author, authorType) = getOperation()
            log.debug("{}:{} - Removing nodes: {}", authorType, author, nodes.map { it.getName() })
            return loadChildren(parent).removeAll(nodes)
        }
    }

    fun getChildren(parent: Node): Stream<Node> {
        readLock.runIn {
            if (nodes.containsKey(parent)) {
                return getListByParent(parent).toList().stream()
            }
        }

        // node not found in cache, try to load from repository
        writeLock.runIn {
            return loadChildren(parent).toList().stream()
        }
    }

    fun getChildrenSize(parent: Node): Int {
        readLock.runIn {
            if (nodes.containsKey(parent)) {
                return getListByParent(parent).size
            }
        }

        // node not found in cache, try to load from repository
        writeLock.runIn {
            return loadChildren(parent).size
        }
    }

    fun clone(node: Node): Node {
        TODO("Not yet implemented")
    }

    // TODO: add NodeRef cache
    fun getNodeRef(node: NodeIml): NodeRef = NodeRefImpl(node.calcPath()) { path -> findNodeByPath(path) }

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
    private fun loadChildren(parent: Node): MutableList<Node> {
        nodes[parent]?.let {
            return it
        }

        val children = nodeRepo.getChildren(parent.getId())
            .map { elem -> createNode(parent, elem) }
            .collect(Collectors.toList()) // don't use Stream.toList() because it's immutable
        nodes[parent] = children

        return children
    }

    private fun getListByParent(parent: Node): List<Node> = (nodes[parent] as List<Node>? ?: emptyList())

    fun getNextNodeId(): Long = lastIdCounter.incrementAndGet()

    fun applyNode(node: Node): Boolean {
        TODO("Not yet implemented")
    }

    fun subscribeOnEvent(plugin: TezrokPlugin, type: NodeType, handler: Function<NodeEvent, EventResult>) =
        featureManager.subscribeOnEvent(plugin, type, handler)

    fun unsubscribeOnEvent(handler: Function<NodeEvent, EventResult>): Boolean =
        featureManager.unsubscribeOnEvent(handler)

    fun startOperation(type: AuthorType, author: String): NodeOperation {
        val authorType = author to type
        writeLock.runIn {
            operations.add(authorType)
        }

        log.info("Started operation by {}: {}", authorType, author)

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

    private fun getOperation(): Pair<String, AuthorType> =
        operations.lastOrNull() ?: throw TezrokException("Node operation not started")

    private fun createNode(parent: Node, nodeElem: NodeElem): Node {
        val nodeProps = NodePropertiesImpl(nodeElem.properties, propertyValueManager)
        val nodeType = nodeProps.getNodeType()
        val id = if (nodeElem.id > 0) nodeElem.id else getNextNodeId()
        val newNode = NodeIml(id, nodeType, parent, nodeProps, this@NodeSupport)
        nodeProps.setNode(newNode)
        return newNode
    }

    private companion object {
        private val log = LoggerFactory.getLogger(NodeSupport::class.java)
    }
}

private fun Node.canAddDuplicateNode(): Boolean =
    getProperties().getBooleanProperty(PropertyName.DuplicateNode, false)

/**
 * Author's type of node operation
 */
internal enum class AuthorType {
    System,
    User,
    Plugin
}
