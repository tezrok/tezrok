package com.tezrok.api.type

import com.tezrok.api.service.NodeService

/**
 * All type definitions
 */
interface TypeFolder : NodeService {
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
