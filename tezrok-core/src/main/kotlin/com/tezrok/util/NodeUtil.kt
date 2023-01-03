package com.tezrok.util

import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeElem
import com.tezrok.api.tree.PropertyName
import java.util.*

/**
 * Calculating path for the node
 */
fun Node.calcPath(): String {
    val nodes = LinkedList<Node>()
    var nextNode: Node? = this
    var size = 0

    // parent name not included in path
    while (nextNode != null && !nextNode.isRoot()) {
        size += nextNode.getName().length
        nodes.addFirst(nextNode)
        nextNode = nextNode.getParent()
    }

    if (nodes.isEmpty()) {
        // path to root node
        return "/"
    }

    val sb = StringBuilder(size + nodes.size)
    for (node in nodes) {
        sb.append('/')
        sb.append(node.getName())
    }

    return sb.toString()
}

fun Node.toElem(): NodeElem = NodeElem(id = getId(),
    properties = getProperties().asMap().filter { it.key != PropertyName.Id })
