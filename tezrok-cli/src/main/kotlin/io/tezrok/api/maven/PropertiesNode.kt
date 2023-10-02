package io.tezrok.api.maven

import io.tezrok.api.TezrokProperties
import io.tezrok.api.input.ModuleElem
import java.util.TreeMap

/**
 * Represents a module's properties.
 *
 * Implementation of [TezrokProperties] for [ModuleNode].
 */
internal class PropertiesNode(private val moduleElem: ModuleElem) : TezrokProperties {
    override fun getProperties(): Map<String, String?> = readonlyMap().toMap()

    override fun getProperty(key: String): String? = readonlyMap()[key]

    override fun setProperty(key: String, value: String?): String? = writableMap().put(key, value)

    override fun removeProperty(key: String): String? = writableMap().remove(key)

    override fun getPropertyNames(): Set<String> = readonlyMap().keys.toSet()

    override fun hasProperty(key: String): Boolean = readonlyMap().containsKey(key)

    private fun writableMap(): MutableMap<String, String?> {
        // properties should be sorted by key, so we use TreeMap
        if (moduleElem.properties == null) {
            moduleElem.properties = TreeMap()
        } else if (moduleElem.properties !is TreeMap) {
            moduleElem.properties = TreeMap(moduleElem.properties)
        }
        return moduleElem.properties!!
    }

    private fun readonlyMap(): Map<String, String?> = moduleElem.properties ?: emptyMap()
}
