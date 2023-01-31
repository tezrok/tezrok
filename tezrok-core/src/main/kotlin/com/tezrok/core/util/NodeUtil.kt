package com.tezrok.core.util

import com.tezrok.api.node.Node
import com.tezrok.api.node.NodeElem
import com.tezrok.api.node.PropertyName
import com.tezrok.core.tree.AuthorType
import java.time.OffsetDateTime
import java.util.*

/**
 * Calculating path for the node
 */
internal fun Node.calcPath(): String {
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

internal fun Node.toElem(): NodeElem = NodeElem(id = getId(),
    properties = getProperties().asMap().filter { it.key != PropertyName.Id })

internal fun Node.createdAt(): OffsetDateTime? =
    getProperties().getProperty(PropertyName.CreatedAt, OffsetDateTime::class.java)

internal fun Node.createdAt(value: OffsetDateTime) =
    getProperties().setProperty(PropertyName.CreatedAt, value)

internal fun Node.updatedAt(): OffsetDateTime? =
    getProperties().getProperty(PropertyName.UpdatedAt, OffsetDateTime::class.java)!!

internal fun Node.updatedAt(value: OffsetDateTime) =
    getProperties().setProperty(PropertyName.UpdatedAt, value)

internal fun Node.author(): String? =
    getProperties().getProperty(PropertyName.Author)

internal fun Node.author(value: String) =
    getProperties().setProperty(PropertyName.Author, value)

internal fun Node.authorType(): AuthorType? =
    getProperties().getProperty(PropertyName.AuthorType, AuthorType::class.java)

internal fun Node.authorType(value: AuthorType) =
    getProperties().setProperty(PropertyName.AuthorType, value)
