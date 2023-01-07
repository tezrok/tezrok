package com.tezrok.core.tree

import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeManager
import com.tezrok.api.tree.NodeRepository
import com.tezrok.core.tree.repo.file.FileNodeRepository
import com.tezrok.core.feature.FeatureManager
import com.tezrok.core.util.toElem
import java.util.stream.Stream

/**
 * Implementation of [NodeManager]
 */
internal class NodeManagerImpl(
    private val nodeRepo: NodeRepository,
    private val featureManager: FeatureManager
) : NodeManager {
    private val nodeSupport = NodeSupport(nodeRepo, featureManager)

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

    override fun findNodeByPath(path: String): Node? = nodeSupport.findNodeByPath(path)
}
