package com.tezrok.core.type

import com.tezrok.api.type.BaseType
import com.tezrok.api.type.TypeField

/**
 * Implementation of [TypeField]
 */
internal class CoreTypeField : TypeField {
    private lateinit var name: String
    private var type: BaseType? = null
    private var optional: Boolean = false
    private var list: Boolean = false
    private var unique: Boolean = false
    private var defValue: Any? = null

    override fun getName(): String = name

    override fun setName(name: String) {
        this.name = name
    }

    override fun getType(): BaseType = type!!

    override fun setType(type: BaseType) {
        this.type = type
    }

    override fun isOptional(): Boolean = optional

    override fun setOptional(optional: Boolean) {
        this.optional = optional
    }

    override fun isList(): Boolean = list

    override fun setList(list: Boolean) {
        this.list = list
    }

    override fun isUnique(): Boolean = unique

    override fun setUnique(unique: Boolean) {
        this.unique = unique
    }

    override fun getDefault(): Any? = defValue

    override fun setDefault(defValue: Any?) {
        this.defValue = defValue
    }
}
