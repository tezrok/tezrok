package io.tezrok.api.maven

import io.tezrok.api.TezrokProperties
import io.tezrok.api.input.ModuleElem

/**
 * Represents a module's properties.
 *
 * Implementation of [TezrokProperties] for [ModuleNode].
 */
internal class PropertiesNode(private val moduleElem: ModuleElem) : TezrokProperties {
    val sd: ArrayList<String> = arrayListOf()
    override fun getProperties(): Map<String, String?> = internalMap().toMap()

    override fun getProperty(key: String): String? = internalMap()[key]

    override fun setProperty(key: String, value: String?): String? = internalMap().put(key, value)

    override fun removeProperty(key: String): String? = internalMap().remove(key)

    override fun getPropertyNames(): Set<String> = internalMap().keys.toSet()

    private fun internalMap(): MutableMap<String, String?> {
        if (moduleElem.properties == null) {
            moduleElem.properties = mutableMapOf()
        }
        return moduleElem.properties!!
    }
}
