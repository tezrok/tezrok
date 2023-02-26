package com.tezrok.api.node

/**
 * Provides information about node issue
 */
interface NodeIssue {
    /**
     * Returns type of the issue
     */
    fun getType(): NodeIssueType

    /**
     * Returns message of the issue
     */
    fun getMessage(): String

    /**
     * Returns list of fixes for the issue
     */
    fun getFixes(): List<NodeIssueFix>
}

/**
 * Provides information about node issue fix
 */
interface NodeIssueFix {
    /**
     * Returns description of the fix for user
     */
    fun getDescription(): String

    /**
     * Try to fix issue. Returns true if issue was fixed
     */
    fun fix(): Boolean
}

/**
 * Types of [NodeIssue]
 */
enum class NodeIssueType {
    /**
     * Warning - node can be used
     */
    WARNING,

    /**
     * Error - node cannot be used
     */
    ERROR
}
