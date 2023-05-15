package io.tezrok.api.maven

import io.tezrok.api.java.JavaRootNode
import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.BaseNode
import io.tezrok.api.node.DirectoryNode

/**
 * Represents directory: src/main
 */
open class MainNode(parent: BaseNode?) : DirectoryNode("main", parent) {
    val java: JavaRootNode = JavaRootNode(this)

    val resources: ResourcesNode = ResourcesNode(this)

    override fun getFiles(): List<BaseFileNode> = listOf(java, resources)
}
