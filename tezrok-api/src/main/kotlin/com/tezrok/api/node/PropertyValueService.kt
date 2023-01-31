package com.tezrok.api.node

import com.tezrok.api.service.TezrokService

/**
 * Service provider for [PropertyValue]
 */
interface PropertyValueService : TezrokService {
    fun getSupportedTypes(): List<Class<Any>>

    fun getPropertyType(clazz: Class<Any>): PropertyValue
}
