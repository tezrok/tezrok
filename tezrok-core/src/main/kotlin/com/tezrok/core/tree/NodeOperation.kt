package com.tezrok.core.tree

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
    val type: String

    /**
     * Stop the operation
     */
    fun stop()
}
