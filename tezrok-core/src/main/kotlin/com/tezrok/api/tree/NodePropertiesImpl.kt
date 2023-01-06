package com.tezrok.api.tree

import com.tezrok.api.TezrokService
import com.tezrok.api.error.TezrokException
import java.util.concurrent.ConcurrentHashMap

/**
 * Internal implementation of the [NodeProperties]
 */
class NodePropertiesImpl(props: Map<PropertyName, String?>) : NodeProperties {
    private val properties: MutableMap<PropertyName, String?> = ConcurrentHashMap(props)
    private var _node: Node? = null

    fun setNode(node: Node) {
        _node = node
    }

    override fun getNode(): Node = _node ?: throw TezrokException("Node is not set")

    override fun isDisabled(): Boolean = getBooleanProperty(PropertyName.Disabled, false)

    override fun setDisabled(disabled: Boolean): Boolean =
        setProperty(PropertyName.Disabled, disabled.toString()) as Boolean? ?: false

    override fun isDeleted(): Boolean = getBooleanProperty(PropertyName.Deleted, false)

    override fun isInfinite(): Boolean = getBooleanProperty(PropertyName.Infinite, false)

    override fun isReadonly(): Boolean = getBooleanProperty(PropertyName.Readonly, false)

    override fun isTransient(): Boolean = getBooleanProperty(PropertyName.Transient, false)

    override fun hasErrors(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getErrors(): List<NodeError> {
        TODO("Not yet implemented")
    }

    override fun setProperty(name: PropertyName, value: String?): String? {
        if (!canEdit(name)) {
            throw TezrokException("Property cannot be edited: $name")
        }

        val oldProp = properties.put(name, value)
        // TODO: check input
        return oldProp
    }

    override fun <T> setProperty(name: PropertyName, value: T): T? {
        TODO("Not yet implemented")
    }

    override fun getProperty(name: PropertyName): String? = getKnownProperty(name) ?: properties[name]

    override fun <T> getProperty(name: PropertyName, clazz: Class<T>): T? {
        if (clazz == String::class.java) {
            return getProperty(name) as T
        }
        if (clazz == Boolean::class.java) {
            return getBooleanProperty(name, false) as T
        }
        // TODO: other types: Int, Double, etc
        // TODO: use PropertyValue to convert

        TODO("Not yet implemented")
    }

    override fun getPropertiesNames(): Set<PropertyName> = properties.keys + getKnownPropertiesNames()

    override fun <T : TezrokService> getService(name: PropertyName, clazz: Class<T>): T? {
        TODO("Not yet implemented")
    }

    override fun can(action: NodeAction, name: PropertyName): Boolean {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NodePropertiesImpl

        if (properties != other.properties) return false

        return true
    }

    override fun hashCode(): Int {
        return properties.hashCode()
    }

    private fun getKnownProperty(name: PropertyName): String? {
        // TODO: check when node is not set
        if (_node == null) {
            return null
        }

        return when (name) {
            PropertyName.Id -> getNode().getId().toString()
            PropertyName.Type -> getNode().getType().name
            else -> null
        }
    }

    private fun getKnownPropertiesNames(): Set<PropertyName> = setOf(PropertyName.Id, PropertyName.Type)
}

internal fun NodeProperties.getNodeType(): NodeType =
    NodeType.getOrCreate(this.getStringProperty(PropertyName.Type, null))
