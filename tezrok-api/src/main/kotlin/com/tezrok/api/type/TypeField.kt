package com.tezrok.api.type

/**
 * Field of a [TypeDef]
 */
interface TypeField {
    /**
     * Returns name of the field
     */
    fun getName(): String

    /**
     * Sets name of the field
     */
    fun setName(name: String)

    /**
     * Returns type of the field
     */
    fun getType(): BaseType

    /**
     * Sets type of the field
     */
    fun setType(type: BaseType)

    fun isOptional(): Boolean

    fun setOptional(optional: Boolean)

    fun isList(): Boolean

    fun setList(list: Boolean)

    fun isUnique(): Boolean

    fun setUnique(unique: Boolean)

    fun getDefault(): Any?

    fun setDefault(defValue: Any?)
}
