package com.tezrok.core.node

/**
 * Represents a current operation
 */
internal interface NodeOperation {
    /**
     * Author of the operation
     */
    val author: String

    /**
     * Author type of the operation
     */
    val type: AuthorType

    /**
     * Stop the operation
     */
    fun stop()
}
