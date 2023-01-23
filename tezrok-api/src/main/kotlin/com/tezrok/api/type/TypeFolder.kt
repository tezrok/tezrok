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
}
