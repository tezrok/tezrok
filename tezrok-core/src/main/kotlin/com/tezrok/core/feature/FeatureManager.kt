package com.tezrok.core.feature

import com.tezrok.api.plugin.TezrokPlugin
import com.tezrok.api.event.EventResult
import com.tezrok.api.event.NodeEvent
import com.tezrok.api.event.ResultType
import com.tezrok.api.feature.Feature
import com.tezrok.api.feature.FeatureService
import com.tezrok.api.tree.NodeType
import com.tezrok.core.plugin.PluginInternalPluginSupport
import com.tezrok.core.plugin.PluginManager
import com.tezrok.core.tree.AuthorType
import com.tezrok.core.tree.NodeManagerImpl
import com.tezrok.core.tree.NodeSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Function

/**
 * The feature manager is responsible for managing the [Feature]s
 */
internal class FeatureManager(pluginManager: PluginManager) {
    private val allFeatures: Map<NodeType, List<Pair<Feature, TezrokPlugin>>> = loadFeatures(pluginManager)
    private val allSubscribers: MutableMap<NodeType, MutableList<Subscriber>> = HashMap()
    private lateinit var manager: NodeManagerImpl

    init {
        log.info("FeatureManager initialized")
    }

    /**
     * Returns all [Feature]s by [NodeType]
     */
    @Synchronized
    private fun getFeatures(nodeType: NodeType): List<Pair<Feature, TezrokPlugin>> {
        return allFeatures[nodeType] ?: emptyList()
    }

    @Synchronized
    private fun getSubscribers(nodeType: NodeType): List<Subscriber> {
        return allSubscribers[nodeType]?.toList() ?: emptyList()
    }

    @Synchronized
    fun setNodeSupport(nodeSupport: NodeSupport) {
        allFeatures.values.flatten().map { it.second }.toSet().forEach { plugin ->
            plugin.setInternalPluginSupport(PluginInternalPluginSupport(plugin, nodeSupport))
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

        val subscribers = getSubscribers(event.type) + getSubscribers(NodeType.Any)

        subscribers.forEach { subscriber ->
            val operation = manager.startOperation(AuthorType.Plugin, subscriber.plugin.getName())

            try {
                val result = subscriber.handler.apply(event)
                if (result.type != ResultType.CONTINUE) {
                    log.warn(
                        "{}:{} - Subscriber {} returned {} for event {}",
                        operation.type, operation.author, subscriber.handler, result.type, event
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

    @Synchronized
    fun subscribeOnEvent(plugin: TezrokPlugin, type: NodeType, handler: Function<NodeEvent, EventResult>) {
        allSubscribers.computeIfAbsent(type) { mutableListOf() }
            .add(Subscriber(plugin, handler))
    }

    @Synchronized
    fun unsubscribeOnEvent(handler: Function<NodeEvent, EventResult>): Boolean {
        return allSubscribers.keys
            .mapNotNull { allSubscribers[it]?.removeIf { subscriber -> subscriber.handler == handler } }
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
                    service!!.getSupportedNodeTypes()
                        .map { type -> Triple(plugin, type, service.getFeatures(type)) }
                }
                .forEach { (plugin, type, featuresList) ->
                    features.getOrPut(type) { mutableListOf() }.addAll(featuresList.map { it to plugin })
                }

            return features
        }

        val log: Logger = LoggerFactory.getLogger(FeatureManager::class.java)
    }

    data class Subscriber(val plugin: TezrokPlugin, val handler: Function<NodeEvent, EventResult>)
}
