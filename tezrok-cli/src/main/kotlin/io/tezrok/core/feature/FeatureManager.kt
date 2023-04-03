package io.tezrok.core.feature

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.node.ProjectNode
import io.tezrok.liquibase.LiquibaseGenerator

internal class FeatureManager {
    private val features: MutableList<TezrokFeature> = mutableListOf()

    init {
        // TODO: load features from configuration
        features.add(LiquibaseGenerator())
    }

    fun applyAll(project: ProjectNode, context: GeneratorContext) {
        features.forEach { feature -> feature.apply(project, context) }
    }
}
