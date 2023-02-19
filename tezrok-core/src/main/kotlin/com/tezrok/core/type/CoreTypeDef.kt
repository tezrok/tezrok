package com.tezrok.core.type

import com.tezrok.api.error.TezrokException
import com.tezrok.api.type.TypeDef
import com.tezrok.api.type.TypeField

/**
 * Implementation of [TypeDef]
 */
internal class CoreTypeDef(private val name: String) : TypeDef {
    private val fields: MutableList<TypeField> = mutableListOf()

    @Synchronized
    override fun getFields(): List<TypeField> = fields.toList()

    @Synchronized
    override fun addField(name: String): TypeField {
        fields.find { it.getName() == name }
            ?.let { throw TezrokException("Field with name $name already exists") }

        val field = CoreTypeField()
        field.setName(name)
        return field
    }

    @Synchronized
    override fun removeField(field: TypeField): Boolean {
        return fields.remove(field)
    }

    override fun getName(): String = name
}
