package com.tezrok.api.type

/**
 * Type definition - standard structure of type
 */
interface TypeDef : BaseType {
    /**
     * Returns all fields of the type
     */
    fun getFields(): List<TypeField>

    /**
     * Add new field
     *
     * @param name Name of the field
     * @return New field
     */
    fun addField(name: String): TypeField

    /**
     * Remove specified field from the type
     */
    fun removeField(field: TypeField): Boolean
}
