package com.tezrok.api.tree

import com.tezrok.api.error.TezrokException
import org.apache.commons.lang3.Validate
import java.util.concurrent.ConcurrentHashMap

class NodePropertiesImpl(props: Map<PropertyName, String?>) : NodeProperties {
    private val properties: MutableMap<PropertyName, Any?> = ConcurrentHashMap(props)
    private var _node: Node? = null

    fun setNode(node: Node) {
        _node = node
    }

    override fun getNode(): Node = _node ?: throw TezrokException("Node is not set")

    override fun isDisabled(): Boolean = getBooleanPropertySafe(PropertyName.Disabled) ?: false

    override fun setDisabled(disabled: Boolean): Boolean =
        setProperty(PropertyName.Disabled, disabled) as Boolean? ?: false

    override fun isDeleted(): Boolean = getBooleanPropertySafe(PropertyName.Deleted) ?: false

    override fun isInfinite(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isReadonly(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTransient(): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasErrors(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getErrors(): List<NodeError> {
        TODO("Not yet implemented")
    }

    override fun setProperty(name: PropertyName, value: Any?): Any? {
        if (!canEdit(name)) {
            throw TezrokException("Property cannot be edited: $name")
        }

        val oldProp = properties.put(name, value)
        // TODO: check input
        return oldProp
    }

    override fun getProperty(name: PropertyName): Any? = getKnownProperty(name) ?: properties[name]

    override fun getPropertiesNames(): Set<PropertyName> = properties.keys + getKnownPropertiesNames()

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

    private fun getKnownProperty(name: PropertyName): Any? {
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

internal fun NodeProperties.getStringPropSafe(name: PropertyName): String? = getProperty(name) as String?

internal fun NodeProperties.getBooleanPropertySafe(name: PropertyName): Boolean? =
    getProperty(name)?.let { "true" == it }

internal fun NodeProperties.getStringProp(name: PropertyName): String =
    Validate.notBlank(getStringPropSafe(name), "Property '%s' cannot be empty", name)!!
