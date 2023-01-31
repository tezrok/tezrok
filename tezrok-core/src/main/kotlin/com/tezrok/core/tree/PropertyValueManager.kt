package com.tezrok.core.tree

import com.tezrok.api.error.TezrokException
import com.tezrok.api.node.NodeRef
import com.tezrok.api.node.PropertyValue
import com.tezrok.api.node.PropertyValueService
import com.tezrok.core.plugin.PluginManager
import java.time.OffsetDateTime
import java.util.List


/**
 * Encapsulates all property value related logic
 */
internal class PropertyValueManager(
    pluginManager: PluginManager
) {
    lateinit var nodeSupport: NodeSupport
    private val allPropertyValues: Map<Class<Any>, PropertyValue> = loadPropertyValues(pluginManager) { nodeSupport }

    fun <T> fromString(rawString: String, clazz: Class<T>): T? {
        if (clazz.isEnum) {
            return clazz.enumConstants.firstOrNull { it.toString() == rawString }
        }

        val finalClass = getFinalClass(clazz)
        val propValue = allPropertyValues[finalClass]
            ?: throw TezrokException("No property value converter for class $clazz")

        return propValue.fromString(rawString) as T
    }

    fun <T> toString(obj: T): String? {
        if (obj == null) {
            return null
        }
        if (obj is Enum<*>) {
            return obj.name
        }

        val finalClass = getObjFinalClass(obj)
        val propValue = allPropertyValues[finalClass]
            ?: throw TezrokException("No property value converter for class ${obj!!::class.java}")

        return propValue.asString(obj)
    }

    private fun getObjFinalClass(obj: Any): Class<*> = getFinalClass(obj::class.java)

    private fun <T> getFinalClass(clazz: Class<T>) =
        if (List::class.java.isAssignableFrom(clazz))
            List::class.java
        else if (NodeRef::class.java.isAssignableFrom(clazz))
            NodeRef::class.java
        else
            clazz

    private companion object {
        private fun loadPropertyValues(
            pluginManager: PluginManager,
            nodeSupport: () -> NodeSupport
        ): Map<Class<Any>, PropertyValue> {
            val map = pluginManager.getPlugins()
                .mapNotNull { it.getService(PropertyValueService::class.java) }
                .flatMap { service -> service.getSupportedTypes().map { it to service.getPropertyType(it) } }
                .toMap()
                .toMutableMap()

            map[List::class.java as Class<Any>] = ListPropertyValue()
            map[OffsetDateTime::class.java as Class<Any>] = OffsetDateTimePropertyValue()
            map[NodeRef::class.java as Class<Any>] = NodeRefPropertyValue { nodeSupport().findNodeByPath(it) }

            return map
        }
    }
}
