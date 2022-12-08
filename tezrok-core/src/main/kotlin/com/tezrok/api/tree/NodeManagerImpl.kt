package com.tezrok.api.tree

import com.tezrok.api.tree.repo.file.FileNodeRepository
import com.tezrok.util.toElem
import java.util.stream.Stream

/**
 * Implementation of NodeManager
 */
class NodeManagerImpl(private val nodeRepo: NodeRepository) : NodeManager {
    private val nodeSupport = NodeSupport(nodeRepo)

    override fun getRootNode(): Node = nodeSupport.getRoot()

    override fun findNodes(term: String): Stream<Node> {
        TODO("Not yet implemented")
    }

    override fun save() {
        nodeRepo as FileNodeRepository
        nodeRepo.clear()

        val root = nodeSupport.getRoot()
        prepareNodesToRepo(0, root)
        nodeRepo.save()
    }

    private fun prepareNodesToRepo(parentId: Long, node: Node) {
        nodeRepo.put(parentId, node.toElem())
        node.getChildren().forEach { prepareNodesToRepo(node.getId(), it) }
    }

    fun findNodeByPath(path: String): Node? = nodeSupport.findNodeByPath(path)
}
