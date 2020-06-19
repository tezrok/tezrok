package io.tezrok.api.model.node

import io.tezrok.api.error.TezrokException
import org.apache.commons.lang3.Validate
import java.util.*

open class Node(open val name: String,
                open val kind: String,
                open val description: String,
                open val parent: Node?,
                open val id: UUID = UUID.randomUUID()) {
    private val _children = mutableListOf<Node>()
    private val _properties = mutableListOf<Property>()

    open val project: ProjectNode
        get() = getParentByType(ProjectNode::class.java)

    open val module: ModuleNode
        get() = getParentByType(ModuleNode::class.java)

    open fun children(): List<Node> = _children

    open fun properties(): List<Property> = _properties

    open fun add(child: Node) {
        Validate.isTrue(child.parent === this, "Invalid parent property")

        _children.add(child)
    }

    open fun remove(child: Node) {
        _children.remove(child)
    }

    open fun add(property: Property) {
        _properties.add(property)
    }

    open fun remove(property: Property) {
        _properties.remove(property)
    }

    private fun <T : Node> getParentByType(clazz: Class<T>): T {
        var parent = parent

        while (parent != null) {
            if (parent.javaClass === clazz) {
                return parent as T
            }
            parent = parent.parent
        }

        throw TezrokException("Parent not found by type: ${clazz.name}")
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
