package com.tezrok.api.tree

import java.util.stream.Stream

/**
 * Implementation of NodeManager
 */
class NodeManagerImpl(private val nodeRepo: NodeRepository) : NodeManager {
    override fun getRootNode(): Node {
        TODO("Not yet implemented")
    }

    override fun findNodes(term: String): Stream<Node> {
        TODO("Not yet implemented")
    }
}
