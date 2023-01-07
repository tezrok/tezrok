package com.tezrok.core.tree

import com.tezrok.api.error.TezrokException
import com.tezrok.api.tree.PropertyValue
import com.tezrok.api.tree.PropertyValueService
import com.tezrok.core.plugin.PluginManager

/**
 * Encapsulates all property value related logic
 */
internal class PropertyValueManager(pluginManager: PluginManager) {
    private val allPropertyValues: Map<Class<*>, PropertyValue<Any>> = loadPropertyValues(pluginManager)

    fun <T> fromString(rawString: String, clazz: Class<T>): T? {
        val propValue = allPropertyValues[clazz] ?: throw TezrokException("No property value converter for class $clazz")

        return propValue.fromString(rawString) as T
    }

    fun <T> toString(obj: T): String? {
        if (obj == null) {
            return null
        }

        val propValue = allPropertyValues[obj!!::class.java]
            ?: throw TezrokException("No property value converter for class ${obj!!::class.java}")

        return propValue.asString(obj)
    }

    private companion object {
        private fun loadPropertyValues(pluginManager: PluginManager): Map<Class<*>, PropertyValue<Any>> {
            return pluginManager.getPlugins()
                .mapNotNull { it.getService(PropertyValueService::class.java) }
                .flatMap { service -> service.getSupportedTypes().map { it to service.getPropertyType(it) } }
                .toMap()
        }
    }
}
