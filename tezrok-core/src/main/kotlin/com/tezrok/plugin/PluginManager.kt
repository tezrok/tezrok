package com.tezrok.plugin

import com.tezrok.api.TezrokPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Collections

class PluginManager {
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
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(PluginManager::class.java)
    }
}
