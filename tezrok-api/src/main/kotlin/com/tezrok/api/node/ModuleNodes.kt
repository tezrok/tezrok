package com.tezrok.api.node

/**
 * Node that contains other module nodes
 */
interface ModuleNodes : NodeList<ModuleNode>, Node {
    override fun getParent(): ProjectNode
}
