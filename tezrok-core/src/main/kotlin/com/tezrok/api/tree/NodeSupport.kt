package com.tezrok.api.tree

import com.tezrok.util.runIn
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Stream

class NodeSupport(lastId: Long) {
    private val lastIdCounter = AtomicLong(lastId)
    private val nodes: MutableMap<Node, MutableList<Node>> = HashMap()
    private val lock = ReentrantReadWriteLock()
    private val writeLock = lock.writeLock()
    private val readLock = lock.readLock()

    fun findByPath(path: String): Node? {
        return null
    }

    fun add(parent: Node, info: NodeInfo): Node {
        val properties = HashMap(info.properties)
        properties[PropertyName.Name] = info.name
        properties[PropertyName.Type] = info.type.name

        writeLock.runIn {
            val nextId = lastIdCounter.incrementAndGet()
            properties[PropertyName.Id] = nextId
            // TODO: validate new node

            val node = NodeIml(nextId, parent, properties, this)
            nodes.computeIfAbsent(node) { ArrayList() }.add(node)

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

    private fun getListByParent(parent: Node): List<Node> = (nodes[parent] as List<Node>? ?: emptyList())
}
