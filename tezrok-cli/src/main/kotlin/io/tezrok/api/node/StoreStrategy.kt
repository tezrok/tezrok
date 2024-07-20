package io.tezrok.api.node

/**
 * Represents a strategy for storing [FileNode].
 */
enum class StoreStrategy {
    /**
     * Save the file to the file system without checking if it already exists
     */
    SAVE,

    /**
     * Save the file to the file system only if it does not already exist
     */
    SAVE_IF_NOT_EXISTS,
}
