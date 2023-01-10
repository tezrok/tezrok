package com.tezrok.core.plugin

import com.tezrok.api.TezrokPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Encapsulates all plugins
 *
 * @see TezrokPlugin
 */
internal class PluginManager {
    private val allPlugins: MutableList<TezrokPlugin> = Collections.synchronizedList(mutableListOf())

    init {
        log.info("PluginManager initialized")
    }

    fun getPlugins(): List<TezrokPlugin> {
        return allPlugins
    }

    fun registerPlugin(plugin: TezrokPlugin) {
        // TODO: add ordering in plugins
        allPlugins.add(plugin)
        log.info("Plugin registered: {}, version: {}", plugin.getName(), plugin.getVersion())
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(PluginManager::class.java)
    }
}
