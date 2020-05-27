package io.tezrok.core.generator

import io.tezrok.api.Generator
import io.tezrok.api.GeneratorContext
import io.tezrok.core.feature.FeatureManager
import io.tezrok.core.feature.FeatureTree

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

        trees.forEach { runGenerator(it) }
    }

    private fun runGenerator(tree: FeatureTree) {
        if (tree.dependsOn.isNotEmpty()) {
            tree.dependsOn.forEach { runGenerator(it) }
        }

        tree.generator.generate()
    }
}
