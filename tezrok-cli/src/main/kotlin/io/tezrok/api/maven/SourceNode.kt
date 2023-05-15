package io.tezrok.api.maven

import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.BaseNode
import io.tezrok.api.node.DirectoryNode

/**
 * Represents directory: src
 */
open class SourceNode(parent: BaseNode?) : DirectoryNode("src", parent) {
    val main: MainNode = MainNode(this)

    override fun getFiles(): List<BaseFileNode> = listOf(main)
}
