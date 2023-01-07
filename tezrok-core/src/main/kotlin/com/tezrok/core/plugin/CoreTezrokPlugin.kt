package com.tezrok.core.plugin

import com.tezrok.api.TezrokPlugin
import com.tezrok.api.TezrokService
import com.tezrok.api.feature.FeatureService
import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeType
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

    private companion object {
        val log: Logger = LoggerFactory.getLogger(CoreTezrokPlugin::class.java)
    }
}
