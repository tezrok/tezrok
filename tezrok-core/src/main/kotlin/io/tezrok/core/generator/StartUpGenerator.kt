package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.Phase
import io.tezrok.api.builder.Builder
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.core.factory.MainExecuteContext
import io.tezrok.core.feature.FeatureManager
import io.tezrok.core.feature.FeatureTree
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter

/**
 * Entry point generator. Create and call all related generators
 */
class StartUpGenerator : Generator {
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
                    parent = context)

            trees.forEach { runGenerator(it, currContext) }
            log.debug("------End phase: {}, module: {}--------", phase, module.name)
        }
    }

    private fun runGenerator(tree: FeatureTree, context: ExecuteContext) {
        if (tree.dependsOn.isNotEmpty()) {
            tree.dependsOn.forEach { runGenerator(it, context) }
        }

        log.debug("Call {}", tree.generator)
        tree.generator.execute(context)
    }

    private class SelectedContext(val selectedPhase: Phase,
                                  val selectedModule: ModuleNode,
                                  val parent: ExecuteContext) : ExecuteContext {
        private val log = LoggerFactory.getLogger(javaClass)

        override fun getPhase(): Phase = selectedPhase

        override fun getModule(): ModuleNode = selectedModule

        override fun <T> getInstance(clazz: Class<T>): T = parent.getInstance(clazz)

        override fun getProject(): ProjectNode = parent.getProject()

        override fun isGenerateTime(): Boolean = parent.isGenerateTime()

        override fun render(builder: Builder) {
            if (getPhase() != Phase.Generate) {
                return
            }

            parent as MainExecuteContext
            val moduleRootDir = File(parent.getTargetDir(), getModule().toMavenVersion().artifactId)
            val targetDir = File(moduleRootDir, builder.path.replace('.', '/'))
            val targetFile = File(targetDir, builder.fileName)

            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            if (!targetFile.exists()) {
                log.debug("Generating {}", targetFile)
                val fw = FileWriter(targetFile)
                builder.build(fw)
                fw.close()
            } else {
                log.warn("Skip generating {}, already exists", targetFile)
            }
        }
    }
}
