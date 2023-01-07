package com.tezrok.core.tree

import com.tezrok.api.error.NodeAlreadyExistsException
import com.tezrok.api.error.TezrokException
import com.tezrok.api.event.ResultType
import com.tezrok.api.event.EventType
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.feature.InternalFeatureSupport
import com.tezrok.api.tree.*
import com.tezrok.core.feature.FeatureManager
import com.tezrok.core.util.calcPath
import com.tezrok.core.util.runIn
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Main implementation of the [Node]
 */
internal class NodeSupport(
    private val nodeRepo: NodeRepository,
    private val featureManager: FeatureManager
) : InternalFeatureSupport {
    private val lastIdCounter = AtomicLong(nodeRepo.getLastId())
    private val nodes: MutableMap<Node, MutableList<Node>> = HashMap()
    private val lock = ReentrantReadWriteLock()
    private val writeLock = lock.writeLock()
    private val readLock = lock.readLock()
    private val root = lazy { createRoot() }

    init {
        featureManager.setInternalFeatureSupport(this)
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

            val eventResult = featureManager.onNodeEvent(NodeEvent(EventType.PreAdd, type, parent, null))

            if (eventResult.type == ResultType.CANCEL) {
                val message = if (eventResult.message.isNullOrBlank()) "Node cannot be added" else eventResult.message
                throw TezrokException(message)
            }

            val newNode = createNode(parent, NodeElem.of(name, type))
            // TODO: validate new node
            nodes.computeIfAbsent(parent) { ArrayList() }.add(newNode)
            featureManager.onNodeEvent(NodeEvent(EventType.PostAdd, type, parent, newNode))

            return newNode
        }
    }

    fun remove(parent: Node, nodes: List<Node>): Boolean {
        writeLock.runIn {
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
            val nodeProps = NodePropertiesImpl(rootElem.properties)
            val node = NodeIml(rootElem.id, NodeType.Root, null, nodeProps, this)
            nodeProps.setNode(node)
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

    override fun getNextNodeId(): Long = lastIdCounter.incrementAndGet()

    override fun applyNode(node: Node): Boolean {
        TODO("Not yet implemented")
    }

    private fun createNode(parent: Node, nodeElem: NodeElem): Node {
        val nodeProps = NodePropertiesImpl(nodeElem.properties)
        val nodeType = nodeProps.getNodeType()
        val id = if (nodeElem.id > 0) nodeElem.id else getNextNodeId()
        val newNode = NodeIml(id, nodeType, parent, nodeProps, this@NodeSupport)
        nodeProps.setNode(newNode)
        return newNode
    }
}

private fun Node.canAddDuplicateNode(): Boolean =
    getProperties().getBooleanProperty(PropertyName.DuplicateNode, false)
