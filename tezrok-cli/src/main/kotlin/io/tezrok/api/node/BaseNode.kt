package io.tezrok.api.node

import org.slf4j.LoggerFactory

/**
 * Common implementation for all nodes
 */
abstract class BaseNode(private var name: String, private val parent: Node?) : Node {
    protected val log = LoggerFactory.getLogger(this.javaClass)!!

    /**
     * Returns the name of the node
     */
    override fun getName(): String = name

    /**
     * Sets the name of the node
     */
    override fun setName(name: String) {
        // TODO: validate name
        this.name = name
    }

    override fun getParent(): Node? = parent
}

