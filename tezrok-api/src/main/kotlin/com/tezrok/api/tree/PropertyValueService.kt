package com.tezrok.api.tree

import com.tezrok.api.TezrokService

/**
 * Service provider for [PropertyValue]
 */
interface PropertyValueService : TezrokService {
    fun getSupportedTypes(): List<Class<Any>>

    fun getPropertyType(clazz: Class<Any>): PropertyValue
}
