package com.tezrok.api.tree

import com.tezrok.api.error.TezrokException
import org.apache.commons.lang3.Validate
import java.util.concurrent.ConcurrentHashMap

class NodePropertiesImpl(props: Map<PropertyName, Any?>, private val node: Node) : NodeProperties {
    private val properties: MutableMap<PropertyName, Any?> = ConcurrentHashMap(props)

    override fun getNode(): Node = node

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

    override fun getProperty(name: PropertyName): Any? = properties[name]

    override fun getPropertiesNames(): Set<PropertyName> = properties.keys

    override fun can(action: NodeAction, name: PropertyName): Boolean {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NodePropertiesImpl

        if (node.getId() != other.node.getId()) return false
        if (properties != other.properties) return false

        return true
    }

    override fun hashCode(): Int {
        var result = node.getId().hashCode()
        result = 31 * result + properties.hashCode()
        return result
    }
}

internal fun NodeProperties.getStringPropSafe(name: PropertyName): String? = getProperty(name) as String?

internal fun NodeProperties.getBooleanPropertySafe(name: PropertyName) = getProperty(name) as Boolean?

internal fun NodeProperties.getStringProp(name: PropertyName): String =
    Validate.notBlank(getStringPropSafe(name), "Property '%s' cannot be empty", name)!!
