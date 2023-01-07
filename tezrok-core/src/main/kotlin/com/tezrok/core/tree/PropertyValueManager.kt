package com.tezrok.core.tree

import com.tezrok.api.error.TezrokException
import com.tezrok.api.tree.PropertyValue
import com.tezrok.api.tree.PropertyValueService
import com.tezrok.core.plugin.PluginManager
import com.tezrok.core.util.JsonUtil
import java.util.Collections
import java.util.List


/**
 * Encapsulates all property value related logic
 */
internal class PropertyValueManager(pluginManager: PluginManager) {
    private val allPropertyValues: Map<Class<Any>, PropertyValue> = loadPropertyValues(pluginManager)

    fun <T> fromString(rawString: String, clazz: Class<T>): T? {
        val finalClass = if (List::class.java.isAssignableFrom(clazz)) List::class.java else clazz
        val propValue = allPropertyValues[finalClass]
            ?: throw TezrokException("No property value converter for class $clazz")

        return propValue.fromString(rawString) as T
    }

    fun <T> toString(obj: T): String? {
        if (obj == null) {
            return null
        }

        val finalClass = getFinalClass<T>(obj)
        val propValue = allPropertyValues[finalClass]
            ?: throw TezrokException("No property value converter for class ${obj!!::class.java}")

        return propValue.asString(obj)
    }

    private fun <T> getFinalClass(obj: T & Any) = if (obj is List<*>) List::class.java else obj.javaClass

    private companion object {
        private fun loadPropertyValues(pluginManager: PluginManager): Map<Class<Any>, PropertyValue> {
            val map = pluginManager.getPlugins()
                .mapNotNull { it.getService(PropertyValueService::class.java) }
                .flatMap { service -> service.getSupportedTypes().map { it to service.getPropertyType(it) } }
                .toMap()
                .toMutableMap()

            map[List::class.java as Class<Any>] = ListPropertyValue()

            return map
        }
    }

    private class ListPropertyValue : PropertyValue {
        private val mapper = JsonUtil.createMapper()

        override fun fromString(value: String): Any? {
            val listType = mapper.typeFactory.constructCollectionType(MutableList::class.java, String::class.java)
            return Collections.unmodifiableList(mapper.readValue(value, listType) as MutableList<String>)
        }

        override fun asString(value: Any): String = mapper.writeValueAsString(value)
    }
}
