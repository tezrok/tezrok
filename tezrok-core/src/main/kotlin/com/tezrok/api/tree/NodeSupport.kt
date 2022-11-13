package com.tezrok.api.tree

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
        return null;
    }

    fun add(parent: Node, info: NodeInfo): Node {
        val properties = HashMap(info.properties)
        properties[NodeProperty.Name] = info.name
        properties[NodeProperty.Type] = info.type.name

        writeLock.lock()
        try {
            val nextId = lastIdCounter.incrementAndGet()
            properties[NodeProperty.Id] = nextId
            // TODO: validate new node

            val node = NodeIml(nextId, parent, properties, this)
            nodes.computeIfAbsent(node) { ArrayList() }.add(node)

            return node
        } finally {
            writeLock.unlock()
        }
    }

    fun remove(parent: Node, nodes: List<Node>): Boolean {
        writeLock.lock()
        try {
            return this.nodes[parent]?.removeAll(nodes) ?: false
        } finally {
            writeLock.unlock()
        }
    }

    fun getChildren(parent: Node): Stream<Node> {
        readLock.lock()
        return try {
            (nodes[parent] as List<Node>? ?: emptyList()).toList().stream()
        } finally {
            readLock.unlock()
        }
    }
}
