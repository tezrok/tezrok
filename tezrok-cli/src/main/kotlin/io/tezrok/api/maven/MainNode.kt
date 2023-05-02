package io.tezrok.api.maven

import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.BaseNode
import io.tezrok.api.node.DirectoryNode

/**
 * Represents the main directory
 */
open class MainNode(parent: BaseNode?) : DirectoryNode("main", parent) {
    val resources: ResourcesNode = ResourcesNode(this)

    override fun getFiles(): List<BaseFileNode> = listOf(resources)
}
