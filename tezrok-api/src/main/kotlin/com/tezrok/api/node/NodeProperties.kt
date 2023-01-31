package com.tezrok.api.node

import com.tezrok.api.service.FileRef
import com.tezrok.api.service.TezrokService
import com.tezrok.api.error.TezrokException

/**
 * Node properties
 */
interface NodeProperties {
    /**
     * Returns corresponding node
     */
    fun getNode(): Node

    /**
     * Returns true if node is disabled
     *
     * Disabled nodes are visible and ignored
     */
    fun isDisabled(): Boolean

    /**
     * Returns true if node is deleted
     *
     * Deleted nodes are not visible and ignored
     */
    fun isDeleted(): Boolean

    /**
     * Return true if node is enabled and not deleted
     */
    fun isActive(): Boolean = !isDisabled() && !isDeleted()

    /**
     * Disable node if it's possible
     */
    fun setDisabled(disabled: Boolean): Boolean

    /**
     * This node cannot be saved. It's generated only in memory
     */
    fun isTransient(): Boolean

    /**
     * Returns true if children of the node can be of infinite size
     */
    fun isInfinite(): Boolean

    /**
     * Node properties and it's children cannot be edited
     */
    fun isReadonly(): Boolean

    /**
     * This node contains errors. Error nodes are ignored
     */
    fun hasErrors(): Boolean

    /**
     * Returns errors
     */
    fun getErrors(): List<NodeError>

    /**
     * Updates property
     *
     * @return previous property value
     */
    fun setProperty(name: PropertyName, value: String?): String?

    /**
     * Updates property as list of Strings
     */
    fun setListProperty(name: PropertyName, list: List<String>): List<String>

    /**
     * Updates property supported by [PropertyValue]
     */
    fun <T> setProperty(name: PropertyName, value: T): T?

    /**
     * Returns property value or null if property not exists
     */
    fun getProperty(name: PropertyName): String?

    /**
     * Returns property value supplied by [PropertyValue]
     */
    fun <T> getProperty(name: PropertyName, clazz: Class<T>): T?

    /**
     * Returns property values as list of String
     */
    fun getListProperty(name: PropertyName): List<String>

    fun setBooleanProperty(name: PropertyName, value: Boolean): Boolean =
        setProperty(name, value.toString()) == "true"

    fun setIntProperty(name: PropertyName, value: Int): Int =
        setProperty(name, value.toString())?.toInt() ?: 0

    fun setLongProperty(name: PropertyName, value: Long): Long =
        setProperty(name, value.toString())?.toLong() ?: 0L

    fun setDoubleProperty(name: PropertyName, value: Double): Double =
        setProperty(name, value.toString())?.toDouble() ?: 0.0

    /**
     * Returns property value as String or default value if property not exists
     *
     * Throws exception if default value is null and property not exists
     */
    fun getStringProperty(name: PropertyName, defValue: String?): String =
        getProperty(name) ?: defValue ?: throw TezrokException("Property '${name.name}' is not set")

    /**
     * Returns property value as Boolean or default value if property not exists
     *
     * Throws exception if default value is null and property not exists
     */
    fun getBooleanProperty(name: PropertyName, defValue: Boolean?): Boolean =
        getStringProperty(name, defValue?.toString()) == "true"

    /**
     * Returns property value as Integer or default value if property not exists
     *
     * Throws exception if default value is null and property not exists
     */
    fun getIntProperty(name: PropertyName, defValue: Int?): Int =
        getStringProperty(name, defValue?.toString()).toInt()

    /**
     * Returns property value as Long or default value if property not exists
     *
     * Throws exception if default value is null and property not exists
     */
    fun getLongProperty(name: PropertyName, defValue: Long?): Long =
        getStringProperty(name, defValue?.toString()).toLong()

    /**
     * Returns property value as Double or default value if property not exists
     *
     * Throws exception if default value is null and property not exists
     */
    fun getDoubleProperty(name: PropertyName, defValue: Double?): Double =
        getStringProperty(name, defValue?.toString()).toDouble()

    /**
     * Removes property and returns its value
     */
    fun removeProperty(name: PropertyName): String?

    /**
     * Returns names of all properties. Can return names which not set yet (properties schema)
     */
    fun getPropertiesNames(): Set<PropertyName>

    /**
     * Returns [FileRef] if the node supports it
     */
    fun getFile(): FileRef = getService(PropertyName.File, FileRef::class.java)
        ?: throw TezrokException("Node is not File")

    /**
     * Returns property related [TezrokService]
     */
    fun <T : TezrokService> getService(name: PropertyName, clazz: Class<T>): T?

    /**
     * Returns [Map] of all properties
     */
    fun asMap(): Map<PropertyName, String?> = getPropertiesNames()
        .associateWith { getProperty(it) }

    /**
     * Returns true if specified property can be edited
     */
    fun canEdit(name: PropertyName): Boolean = can(NodeAction.Edit, name)

    /**
     * Returns true if specified property can be added
     */
    fun canAdd(name: PropertyName): Boolean = can(NodeAction.Add, name)

    /**
     * Returns true if specified property can be deleted
     */
    fun canDelete(name: PropertyName): Boolean = can(NodeAction.Delete, name)

    /**
     * Ability of custom action
     */
    fun can(action: NodeAction, name: PropertyName): Boolean
}
