package com.tezrok.plugin

import com.tezrok.api.Plugin

class PluginManager(private val plugins: List<Plugin>) {
    private val allPlugins: List<Plugin> = plugins.toList()

    fun getPlugins(): List<Plugin> {
        return allPlugins
    }
}
