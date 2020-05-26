package io.tezrok.core.generator

import io.tezrok.api.Generator
import io.tezrok.api.GeneratorContext
import io.tezrok.core.feature.FeatureManager

/**
 * Entry point generator. Create and call all related generators
 */
class StartUpGenerator(private val context: GeneratorContext) : Generator {
    override fun generate() {
        val project = context.project
        val module = project.modules().first()
        val features = module.features()
        val featureManager = context.getInstance(FeatureManager::class.java)
        val trees = features.map { f -> featureManager.getFeatureTree(f.name) }

        trees.forEach { println(it) }
    }
}
