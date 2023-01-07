package com.tezrok.core.tree.repo.file

import com.tezrok.api.tree.NodeElem
import com.tezrok.api.tree.NodeRepository
import com.tezrok.core.util.JsonUtil
import org.apache.commons.lang3.Validate
import java.io.File
import java.util.stream.Stream

/**
 * File-based repository (json-format)
 */
internal class FileNodeRepository(private val file: File) : NodeRepository {
    // mapping[parentId]=List of child
    private var lastUsedId: Long = 0L;
    private val nodes: MutableMap<Long, MutableMap<Long, NodeElem>> = HashMap()
    private val mapper = JsonUtil.createMapper()

    init {
        load()
    }

    /**
     * Clear inner cache
     */
    fun clear() {
        lastUsedId = 0
        nodes.clear()
    }

    override fun getRoot(): NodeElem = nodes[0]?.values?.first()
        ?: throw IllegalStateException("Root element not found")

    override fun getChildren(parentId: Long): Stream<NodeElem> =
        nodes[parentId]?.values?.stream() ?: Stream.empty()

    override fun put(parentId: Long, node: NodeElem) {
        nodes.computeIfAbsent(parentId) { HashMap() }[node.id] = node
    }

    override fun save() {
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, getRoot().toFileElem())
    }

    override fun getLastId(): Long = lastUsedId

    /**
     * Loads all nodes from file
     */
    private fun load() {
        val root = if (file.exists()) mapper.readValue(file, FileNodeElem::class.java)
            ?: throw IllegalStateException("Root elem not found in file $file")
        else
            FileNodeElem.ROOT

        Validate.isTrue(
            root.id == FileNodeElem.ROOT_ID,
            "Root element id expected ${FileNodeElem.ROOT_ID} but found: %d", root.id
        )

        nodes[0L] = mutableMapOf(0L to root.toElem())
        loadNodes(root)
    }

    private fun loadNodes(elem: FileNodeElem) {
        lastUsedId = Math.max(lastUsedId, elem.id)

        elem.items?.let { items ->
            nodes[elem.id] = items.associate { it.id to it.toElem() }.toMutableMap()
            items.forEach { child -> loadNodes(child) }
        }
    }

    private fun NodeElem.toFileElem(): FileNodeElem =
        FileNodeElem(
            id,
            props = properties.map { it.key.name to it.value }.toMap()
                .let { it.ifEmpty { null } },
            items = getChildren(id).map { it.toFileElem() }.toList()
                .let { it.ifEmpty { null } }
        )
}
