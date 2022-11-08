package com.tezrok.api

/**
 * Plugin of the system
 */
interface Plugin {
    fun getName(): String

    fun getVersion(): String

    fun getService(): Service
}
