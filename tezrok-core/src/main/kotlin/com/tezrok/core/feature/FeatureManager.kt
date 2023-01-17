package com.tezrok.core.feature

import com.tezrok.api.TezrokPlugin
import com.tezrok.api.event.EventResult
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.event.ResultType
import com.tezrok.api.feature.Feature
import com.tezrok.api.feature.FeatureService
import com.tezrok.api.feature.InternalFeatureSupport
import com.tezrok.api.tree.NodeType
import com.tezrok.core.plugin.PluginManager
import com.tezrok.core.tree.NodeManagerImpl
import com.tezrok.core.util.AuthorType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

/**
 * The feature manager is responsible for managing the [Feature]s
 */
internal class FeatureManager(pluginManager: PluginManager) {
    private val allFeatures: Map<NodeType, List<Pair<Feature, TezrokPlugin>>> = loadFeatures(pluginManager)
    private lateinit var manager: NodeManagerImpl
    private val allSubscribers: MutableMap<NodeType, MutableList<Function<NodeEvent, EventResult>>> = ConcurrentHashMap()
    // TODO: call subscribers on event

    init {
        log.info("FeatureManager initialized")
    }

    /**
     * Returns all [Feature]s by [NodeType]
     */
    private fun getFeatures(nodeType: NodeType): List<Pair<Feature, TezrokPlugin>> {
        return allFeatures[nodeType] ?: emptyList()
    }

    fun setInternalFeatureSupport(internalFeatureSupport: InternalFeatureSupport) {
        allFeatures.values.flatten().map { it.second }.toSet().forEach { plugin ->
            plugin.setInternalFeatureSupport(internalFeatureSupport)
        }
        allFeatures.values.flatten().forEach { (feature, _) ->
            feature.setInternalFeatureSupport(internalFeatureSupport)
        }
    }

    /**
     * Fire event on all interested [Feature]s
     */
    fun onNodeEvent(event: NodeEvent): EventResult {
        val features = HashSet(getFeatures(event.type) + getFeatures(NodeType.Any))

        features.forEach { (feature, plugin) ->
            val operation = manager.startOperation(AuthorType.Plugin, plugin.getName())

            try {
                val result = feature.onNodeEvent(event)
                if (result.type != ResultType.CONTINUE) {
                    log.warn(
                        "{}:{} - Feature {} returned {} for event {}",
                        operation.type, operation.author, feature.getName(), result.type, event
                    )
                    return result
                }
            } catch (e: Exception) {
                // TODO: Handle exceptions
                throw e
            } finally {
                operation.stop()
            }
        }

        return EventResult.Continue
    }

    fun setManager(manager: NodeManagerImpl) {
        this.manager = manager
    }

    fun subscribeOnEvent(type: NodeType, handler: Function<NodeEvent, EventResult>) {
        allSubscribers.computeIfAbsent(type) { Collections.synchronizedList(mutableListOf()) }
            .add(handler)
    }

    fun unsubscribeOnEvent(handler: Function<NodeEvent, EventResult>): Boolean {
        return allSubscribers.keys
            .mapNotNull { allSubscribers[it]?.remove(handler) }
            .toList()
            .any { it }
    }

    private companion object {
        /**
         * Loads all features from plugins
         */
        private fun loadFeatures(pluginManager: PluginManager): Map<NodeType, List<Pair<Feature, TezrokPlugin>>> {
            val features = mutableMapOf<NodeType, MutableList<Pair<Feature, TezrokPlugin>>>()

            pluginManager.getPlugins()
                .map { it to it.getService(FeatureService::class.java) }
                .filter { it.second != null }
                .flatMap { (plugin, service) ->
                    service!!.getSupportedNodeTypes().map { type -> Triple(plugin, type, service.getFeatures(type)) }
                }
                .forEach { (plugin, type, featuresList) ->
                    features.getOrPut(type) { mutableListOf() }.addAll(featuresList.map { it to plugin })
                }

            return features
        }

        val log: Logger = LoggerFactory.getLogger(FeatureManager::class.java)
    }
}
