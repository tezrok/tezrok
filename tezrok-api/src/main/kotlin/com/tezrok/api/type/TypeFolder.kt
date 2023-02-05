package com.tezrok.api.type

import com.tezrok.api.service.TezrokService

/**
 * All type definitions
 */
interface TypeFolder : TezrokService {
    /**
     * Returns all [TypeDef]s
     */
    fun getTypes(): List<TypeDef>

    /**
     * Add new [TypeDef]
     */
    fun addType(name: String): TypeDef

    /**
     * Remove specified [TypeDef]
     */
    fun removeType(type: TypeDef): Boolean
}
