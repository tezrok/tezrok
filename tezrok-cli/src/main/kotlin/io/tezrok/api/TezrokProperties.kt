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

    fun hasProperty(key: String): Boolean

    fun setPropertyIfAbsent(key: String, value: String?) {
        if (!hasProperty(key))
            setProperty(key, value)
    }

    fun setPropertyIfPresent(key: String, value: String?) {
        if (hasProperty(key))
            setProperty(key, value)
    }
}
