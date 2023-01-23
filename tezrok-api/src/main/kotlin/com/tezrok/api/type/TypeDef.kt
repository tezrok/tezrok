package com.tezrok.api.type

/**
 * Type definition - standard structure of type
 */
interface TypeDef : BaseType {
    /**
     * Returns all fields of the type
     */
    fun getFields(): List<TypeField>
}
