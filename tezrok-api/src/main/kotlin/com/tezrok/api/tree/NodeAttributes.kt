package com.tezrok.api.tree

/**
 * Node attributes
 */
interface NodeAttributes {
    /**
     * Returns true if node is enabled. Disabled nodes are ignored
     */
    fun isEnabled(): Boolean

    /**
     * Returns true if children of the node can be of infinite size
     */
    fun isInfinite(): Boolean

    /**
     * Node and it's children cannot be edited
     */
    fun isReadonly(): Boolean

    /**
     * This node cannot be saved. It is generated only in memory
     */
    fun isTransient(): Boolean

    /**
     * This node contains errors. Error nodes are ignored
     */
    fun hasErrors(): Boolean

    /**
     * Returns true if specified property can be edited
     */
    fun canEdit(property: NodeProperty): Boolean

    /**
     * Returns true if specified property can be added
     */
    fun canAdd(property: NodeProperty): Boolean

    /**
     * Returns true if specified property can be deleted
     */
    fun canDelete(property: NodeProperty): Boolean

    /**
     * Ability of custom action
     */
    fun can(action: NodeAction, property: NodeProperty): Boolean
}
