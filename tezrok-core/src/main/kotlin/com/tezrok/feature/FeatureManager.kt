package com.tezrok.feature

import com.tezrok.api.feature.Feature
import com.tezrok.api.feature.FeatureService
import com.tezrok.api.tree.NodeType
import com.tezrok.plugin.PluginManager

/**
 * The feature manager is responsible for managing the [Feature]s.
 */
class FeatureManager(private val pluginManager: PluginManager) {
    private val allFeatures: Map<NodeType, List<Feature>> = loadFeatures(pluginManager)

    /**
     * Returns all [Feature]s by [NodeType]
     */
    fun getFeatures(nodeType: NodeType): List<Feature> {
        return allFeatures[nodeType] ?: emptyList()
    }

    private companion object {
        /**
         * Loads all features from plugins
         */
        private fun loadFeatures(pluginManager: PluginManager): Map<NodeType, List<Feature>> {
            val features = mutableMapOf<NodeType, MutableList<Feature>>()

            pluginManager.getPlugins()
                .mapNotNull { it.getService(FeatureService::class.java) }
                .flatMap { service ->
                    service.getSupportedNodeTypes().map { type -> type to service.getFeatures(type) }
                }
                .forEach { (type, featuresList) ->
                    features.getOrPut(type) { mutableListOf() }.addAll(featuresList)
                }

            return features
        }
    }
}
