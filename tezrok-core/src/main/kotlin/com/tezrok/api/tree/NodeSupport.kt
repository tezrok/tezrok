package com.tezrok.api.tree

import com.tezrok.util.calcPath
import com.tezrok.util.runIn
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Stream

class NodeSupport(private val nodeRepo: NodeRepository) {
    private val lastIdCounter = AtomicLong(nodeRepo.getLastId())
    private val nodes: MutableMap<Node, MutableList<Node>> = HashMap()
    private val nodesProperties: MutableMap<Node, NodeProperties> = HashMap()
    private val lock = ReentrantReadWriteLock()
    private val writeLock = lock.writeLock()
    private val readLock = lock.readLock()
    private val root = lazy { createRoot() }

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
        val properties = HashMap<PropertyName, Any?>()
        properties[PropertyName.Name] = name
        properties[PropertyName.Type] = type.name

        writeLock.runIn {
            val nextId = lastIdCounter.incrementAndGet()
            properties[PropertyName.Id] = nextId
            val node = NodeIml(nextId, parent, this)
            nodesProperties[node] = NodePropertiesImpl(properties, node);
            // TODO: validate new node

            nodes.computeIfAbsent(parent) { ArrayList() }.add(node)

            return node
        }
    }

    fun remove(parent: Node, nodes: List<Node>): Boolean {
        writeLock.runIn {
            return this.nodes[parent]?.removeAll(nodes) ?: false
        }
    }

    fun getChildren(parent: Node): Stream<Node> {
        readLock.runIn {
            return getListByParent(parent).toList().stream()
        }
    }

    fun getChildrenSize(parent: Node): Int {
        readLock.runIn {
            return getListByParent(parent).size
        }
    }

    fun clone(node: Node): Node {
        TODO("Not yet implemented")
    }

    private fun createRoot(): Node {
        val rootElem = nodeRepo.getRoot()
        val properties = HashMap(rootElem.properties)
        properties[PropertyName.Id] = rootElem.id

        writeLock.runIn {
            val node = NodeIml(rootElem.id, null, this)
            nodesProperties[node] = NodePropertiesImpl(properties, node)

            return node
        }
    }

    private fun getListByParent(parent: Node): List<Node> = (nodes[parent] as List<Node>? ?: emptyList())

    fun getProperties(node: Node): NodeProperties = readLock.runIn {
        nodesProperties[node] ?: throw IllegalStateException("Properties not found")
    }

    // TODO: add NodeRef cache
    fun getNodeRef(node: NodeIml): NodeRef = NodeRefImpl(node.calcPath()) { path -> findNodeByPath(path) }
}
