package com.tezrok.api.tree.repo.file

import com.fasterxml.jackson.databind.ObjectMapper
import com.tezrok.api.tree.NodeElem
import com.tezrok.api.tree.NodeRepository
import org.apache.commons.lang3.Validate
import java.io.File
import java.util.stream.Stream

/**
 * File-based repository (json-format)
 */
class FileNodeRepository(private val file: File) : NodeRepository {
    private val nodes: MutableMap<Long, MutableMap<Long, NodeElem>> = HashMap()

    /**
     * Loads all nodes from file
     */
    fun load() {
        val mapper = ObjectMapper()
        val root = mapper.readValue(file, FileNodeElem::class.java)
            ?: throw IllegalStateException("Root elem not found in file $file")

        Validate.isTrue(root.id == 0L, "Root element id expected zero but found: %d", root.id)

        nodes[0L] = mutableMapOf(0L to root.toElem())
        loadNodes(root)
    }

    override fun getRoot(): NodeElem = nodes[0]?.getValue(0L) ?: throw IllegalStateException("Root element not found")

    override fun getChildren(parentId: Long): Stream<NodeElem> =
        nodes[parentId]?.values?.stream() ?: Stream.empty()

    override fun put(parentId: Long, node: NodeElem) {
        nodes.computeIfAbsent(parentId) { HashMap() }[node.id] = node
    }

    override fun flush() {
        TODO("Not yet implemented")
    }

    private fun loadNodes(elem: FileNodeElem) {
        nodes[elem.id] = elem.items.associate { it.id to it.toElem() }.toMutableMap()
        elem.items.forEach { child -> loadNodes(child) }
    }
}
