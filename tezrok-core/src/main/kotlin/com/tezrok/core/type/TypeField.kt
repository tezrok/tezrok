package com.tezrok.core.type

interface TypeField {
    /**
     * Returns name of the field
     */
    fun getName(): String

    /**
     * Returns type of the field
     */
    fun getType(): String
}
