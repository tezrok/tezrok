package com.tezrok.api.type

/**
 * Type definition - standard structure of type
 */
interface TypeDef {
    /**
     * Returns name of the type
     */
    fun getName(): String

    /**
     * Returns all fields of the type
     */
    fun getFields(): List<TypeField>
}
