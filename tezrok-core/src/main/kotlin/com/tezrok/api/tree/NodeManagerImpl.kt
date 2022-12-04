package com.tezrok.api.tree

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
}
