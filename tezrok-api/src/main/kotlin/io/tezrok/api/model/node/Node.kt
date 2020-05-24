package io.tezrok.api.model.node

import java.util.*

open class Node(val name: String,
                val kind: String,
                val description: String,
                val id: UUID = UUID.randomUUID()) {
    private val _children = mutableListOf<Node>()
    private val _properties = mutableListOf<Property>()

    open fun children(): List<Node> = _children

    open fun properties(): List<Property> = _properties

    fun add(child: Node) {
        _children.add(child)
    }

    fun remove(child: Node) {
        _children.remove(child)
    }

    fun add(property: Property) {
        _properties.add(property)
    }

    fun remove(property: Property) {
        _properties.remove(property)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Node

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Node(name='$name', kind='$kind', description='$description', id=$id)"
    }
}
