package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.Phase
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.visitor.LogicModelVisitor
import io.tezrok.api.visitor.ModelPhase
import io.tezrok.core.GlobalContextImpl
import io.tezrok.core.factory.Factory
import io.tezrok.core.feature.FeatureManager
import io.tezrok.core.feature.FeatureTree
import org.slf4j.LoggerFactory

/**
 * Entry point generator. Create and call all related generators
 */
class StartUpGenerator(private val factory: Factory) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute() {
        val project = factory.getProject()
        val featureManager = factory.getInstance(FeatureManager::class.java)
        val cache = mutableMapOf<ModuleNode, List<FeatureTree>>()

        applyModelVisitors(project)
        executeOnPhase(project, Phase.Init, cache, featureManager)
        executeOnPhase(project, Phase.Generate, cache, featureManager)
    }

    private fun applyModelVisitors(project: ProjectNode) {
        val context = GlobalContextImpl(factory)

        log.info("Begin model visitors: {}", ModelPhase.Init)
        factory.applyVisitors(LogicModelVisitor::class.java) { visitor ->
            visitor.visit(project, ModelPhase.Init, context)
        }
        log.info("End model visitors: {}", ModelPhase.Init)

        log.info("Begin model visitors: {}", ModelPhase.Edit)
        factory.applyVisitors(LogicModelVisitor::class.java) { visitor ->
            visitor.visit(project, ModelPhase.Edit, context)
        }
        log.info("End model visitors: {}", ModelPhase.Edit)

        log.info("Begin model visitors: {}", ModelPhase.PostEdit)
        factory.applyVisitors(LogicModelVisitor::class.java) { visitor ->
            visitor.visit(project, ModelPhase.PostEdit, context)
        }
        log.info("End model visitors: {}", ModelPhase.PostEdit)
    }

    private fun executeOnPhase(project: ProjectNode, phase: Phase,
                               cache: MutableMap<ModuleNode, List<FeatureTree>>,
                               featureManager: FeatureManager) {

        project.modules().forEach { module ->
            log.debug("------Start phase: {}, module: {}--------", phase, module.name)


            val trees = cache.computeIfAbsent(module) {
                it.features().map { f -> featureManager.getFeatureTree(f.name) }
            }
            val currContext = SelectedContext(selectedPhase = phase,
                    selectedModule = module,
                    factory = factory)

            trees.forEach { runGenerator(it, currContext) }
            log.debug("------End phase: {}, module: {}--------", phase, module.name)
        }
    }

    private fun runGenerator(tree: FeatureTree, context: ExecuteContext) {
        if (tree.dependsOn.isNotEmpty()) {
            tree.dependsOn.forEach { runGenerator(it, context) }
        }

        val generator = tree.service as? Generator

        if (generator != null) {
            log.debug("Call {}", generator::class.java.name)
            generator.execute(context)
        }
    }
}
