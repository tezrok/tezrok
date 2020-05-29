package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.Phase
import io.tezrok.api.builder.Builder
import io.tezrok.api.builder.type.NamedType
import io.tezrok.api.builder.type.Type
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.core.factory.Factory
import io.tezrok.core.factory.MainExecuteContext
import io.tezrok.core.feature.FeatureManager
import io.tezrok.core.feature.FeatureTree
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter

/**
 * Entry point generator. Create and call all related generators
 */
class StartUpGenerator(private val factory: Factory) : Generator {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(context: ExecuteContext) {
        val project = context.getProject()
        val featureManager = context.getInstance(FeatureManager::class.java)
        val cache = mutableMapOf<ModuleNode, List<FeatureTree>>()

        executeOnPhase(project, Phase.Init, cache, featureManager, context)
        executeOnPhase(project, Phase.Generate, cache, featureManager, context)
    }

    private fun executeOnPhase(project: ProjectNode, phase: Phase,
                               cache: MutableMap<ModuleNode, List<FeatureTree>>,
                               featureManager: FeatureManager, context: ExecuteContext) {

        project.modules().forEach { module ->
            log.debug("------Start phase: {}, module: {}--------", phase, module.name)


            val trees = cache.computeIfAbsent(module) {
                it.features().map { f -> featureManager.getFeatureTree(f.name) }
            }
            val currContext = SelectedContext(selectedPhase = phase,
                    selectedModule = module,
                    parent = context,
                    factory = factory)

            trees.forEach { runGenerator(it, currContext) }
            log.debug("------End phase: {}, module: {}--------", phase, module.name)
        }
    }

    private fun runGenerator(tree: FeatureTree, context: ExecuteContext) {
        if (tree.dependsOn.isNotEmpty()) {
            tree.dependsOn.forEach { runGenerator(it, context) }
        }

        log.debug("Call {}", tree.generator::class.java.name)
        tree.generator.execute(context)
    }
}
