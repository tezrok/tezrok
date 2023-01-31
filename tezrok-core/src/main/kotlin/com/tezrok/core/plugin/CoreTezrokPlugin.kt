package com.tezrok.core.plugin

import com.tezrok.api.plugin.TezrokPlugin
import com.tezrok.api.service.TezrokService
import com.tezrok.api.event.EventResult
import com.tezrok.api.feature.FeatureService
import com.tezrok.api.plugin.InternalPluginSupport
import com.tezrok.api.node.Node
import com.tezrok.api.node.NodeType
import com.tezrok.core.service.CoreFeatureService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * [TezrokPlugin] implementation for [Node] of all system types ([NodeType])
 *
 * @see TezrokPlugin
 * @see NodeType
 * @since 1.0
 */
internal class CoreTezrokPlugin : TezrokPlugin {
    private lateinit var internalPluginSupport: InternalPluginSupport
    private val featureService = lazy { CoreFeatureService() }

    init {
        log.info("CoreTezrokPlugin initialized")
    }

    override fun getName(): String {
        return "CoreTezrokPlugin"
    }

    override fun getVersion(): String = "1.0-beta"

    override fun getAuthor(): String = "Tezrok Team"

    @Suppress("UNCHECKED_CAST")
    override fun <T : TezrokService> getService(clazz: Class<T>): T? =
        when (clazz) {
            FeatureService::class.java -> featureService.value as T
            else -> null
        }

    override fun setInternalPluginSupport(internalPluginSupport: InternalPluginSupport) {
        this.internalPluginSupport = internalPluginSupport

        this.internalPluginSupport.subscribeOnEvent(NodeType.Any) { event ->
            log.info("Event {} received", event)
            EventResult.Continue
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(CoreTezrokPlugin::class.java)
    }
}
