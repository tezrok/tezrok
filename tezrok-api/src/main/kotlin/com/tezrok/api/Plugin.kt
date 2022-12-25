package com.tezrok.api

/**
 * Basic interface for all plugins
 */
interface Plugin {
    /**
     * Returns name of the plugin
     */
    fun getName(): String

    /**
     * Returns plugin version
     */
    fun getVersion(): String

    /**
     * Returns service by class if it is supported by the plugin
     */
    fun <T : Service> getService(clazz: Class<T>): T?
}
