package io.tezrok.api

/**
 * Represents a key-value properties.
 */
interface TezrokProperties {
    fun getProperties(): Map<String, String?>

    fun getProperty(key: String): String?

    fun setProperty(key: String, value: String?): String?

    fun removeProperty(key: String): String?

    fun getPropertyNames(): Set<String>
}
