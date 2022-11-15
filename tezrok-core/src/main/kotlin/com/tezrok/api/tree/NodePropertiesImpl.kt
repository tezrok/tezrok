package com.tezrok.api.tree

import org.apache.commons.lang3.Validate
import java.util.concurrent.ConcurrentHashMap

class NodePropertiesImpl(props: Map<PropertyName, Any?>, private val node: Node) : NodeProperties {
    private val properties: MutableMap<PropertyName, Any?> = ConcurrentHashMap(props)

    override fun getNode(): Node = node

    override fun isEnabled(): Boolean = getProperty(PropertyName.Enabled) as Boolean? ?: true

    override fun setEnabled(enabled: Boolean): Boolean = setProperty(PropertyName.Enabled, enabled) as Boolean? ?: true

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
        val oldProp = properties.put(name, value)
        // TODO: check input
        return oldProp
    }

    override fun getProperty(name: PropertyName): Any? = properties[name]

    override fun getPropertiesNames(): Set<PropertyName> = properties.keys

    override fun can(action: NodeAction, name: PropertyName): Boolean {
        TODO("Not yet implemented")
    }

    fun getStringPropSafe(name: PropertyName): String? = properties[name] as String?

    fun getStringProp(name: PropertyName): String =
        Validate.notBlank(getStringPropSafe(name), "Property '%s' cannot be empty", name)!!
}
