package com.tezrok.api.tree

class NodeRefImpl(
    private val path: String,
    private val handler: (String) -> Node?
) : NodeRef {
    override fun getPath(): String = path

    override fun getNode(): Node? = handler(path)
}
