package com.tezrok.core.tree

import com.tezrok.api.node.Node
import com.tezrok.api.node.NodeManager
import com.tezrok.api.node.NodeRepository
import com.tezrok.core.feature.FeatureManager
import com.tezrok.core.tree.repo.file.FileNodeRepository
import com.tezrok.core.util.toElem
import org.slf4j.LoggerFactory
import java.util.stream.Stream

/**
 * Implementation of [NodeManager]
 */
internal class NodeManagerImpl(
    private val nodeRepo: NodeRepository,
    private val featureManager: FeatureManager,
    private val propertyValueManager: PropertyValueManager
) : NodeManager {
    private val nodeSupport = NodeSupport(nodeRepo, featureManager, propertyValueManager)

    init {
        log.info("NodeManager initialized")
        featureManager.setManager(this)
    }

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

    fun startOperation(type: AuthorType, author: String): NodeOperation =
        nodeSupport.startOperation(type, author)

    override fun findNodeByPath(path: String): Node? = nodeSupport.findNodeByPath(path)

    private companion object {
        private val log = LoggerFactory.getLogger(NodeManagerImpl::class.java)
    }
}
