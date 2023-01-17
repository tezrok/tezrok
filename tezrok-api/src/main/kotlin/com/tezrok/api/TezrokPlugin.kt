package com.tezrok.api

import com.tezrok.api.feature.InternalFeatureSupport

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
     * Sets [InternalFeatureSupport] for the plugin
     */
    fun setInternalFeatureSupport(internalFeatureSupport: InternalFeatureSupport)
}
