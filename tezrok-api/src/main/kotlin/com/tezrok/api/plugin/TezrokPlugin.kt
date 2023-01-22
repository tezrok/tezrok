package com.tezrok.api.plugin

import com.tezrok.api.service.TezrokService

/**
 * Basic interface for all plugins
 */
interface TezrokPlugin {
    /**
     * Returns name of the plugin
     */
    fun getName(): String

    /**
     * Returns plugin version
     */
    fun getVersion(): String

    /**
     * Returns author of the plugin
     */
    fun getAuthor(): String

    /**
     * Returns service by class if it is supported by the plugin
     */
    fun <T : TezrokService> getService(clazz: Class<T>): T?

    /**
     * Sets [InternalPluginSupport] for the plugin
     */
    fun setInternalFeatureSupport(internalPluginSupport: InternalPluginSupport)
}
