package com.tezrok.core.tree

import com.tezrok.api.TezrokService
import com.tezrok.api.error.TezrokException
import com.tezrok.api.tree.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Internal implementation of the [NodeProperties]
 */
internal class NodePropertiesImpl(
    props: Map<PropertyName, String?>,
    private val propertyValueManager: PropertyValueManager
) : NodeProperties {
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

        // TODO: check input
        // TODO: send pre & post events
        if (value == null) {
            return properties.remove(name)
        }

        return properties.put(name, value)
    }

    override fun setListProperty(name: PropertyName, list: List<String>): List<String> =
        setProperty(name, list) ?: emptyList()

    override fun <T> setProperty(name: PropertyName, value: T): T? =
        when (value!!::class.java) {
            java.lang.String::class.java, String::class.java -> setProperty(name, value as String) as T
            java.lang.Boolean::class.java, Boolean::class.java -> setBooleanProperty(name, value as Boolean) as T
            java.lang.Integer::class.java, Int::class.java -> setIntProperty(name, value as Int) as T
            java.lang.Long::class.java, Long::class.java -> setLongProperty(name, value as Long) as T
            java.lang.Double::class.java, Double::class.java -> setDoubleProperty(name, value as Double) as T
            else -> {
                val rawString = propertyValueManager.toString(value)
                val oldValue = setProperty(name, rawString)

                oldValue?.let { propertyValueManager.fromString(it, value!!::class.java) }
            }
        }

    override fun getProperty(name: PropertyName): String? = getKnownProperty(name) ?: properties[name]

    override fun <T> getProperty(name: PropertyName, clazz: Class<T>): T? =
        when (clazz) {
            java.lang.String::class.java, String::class.java -> getProperty(name) as T
            java.lang.Boolean::class.java, Boolean::class.java -> getBooleanProperty(name, false) as T
            java.lang.Integer::class.java, Int::class.java -> getIntProperty(name, 0) as T
            java.lang.Long::class.java, Long::class.java -> getLongProperty(name, 0) as T
            java.lang.Double::class.java, Double::class.java -> getDoubleProperty(name, 0.0) as T
            else -> getProperty(name)?.let { propertyValueManager.fromString(it, clazz) }
        }

    override fun getListProperty(name: PropertyName): List<String> {
        return getProperty(name, java.util.List::class.java)?.let { it as List<String> } ?: emptyList()
    }

    override fun removeProperty(name: PropertyName): String? = properties.remove(name)

    override fun getPropertiesNames(): Set<PropertyName> = properties.keys + getKnownPropertiesNames()

    override fun <T : TezrokService> getService(name: PropertyName, clazz: Class<T>): T? {
        TODO("Not yet implemented")
    }

    /**
     * By default, all properties are editable
     */
    override fun can(action: NodeAction, name: PropertyName): Boolean = true

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
