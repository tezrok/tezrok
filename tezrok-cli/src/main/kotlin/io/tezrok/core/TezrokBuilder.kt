package io.tezrok.core

import io.tezrok.core.feature.FeatureManager
import io.tezrok.core.input.ProjectElemRepository
import io.tezrok.core.output.ProjectNodeFactory
import io.tezrok.core.output.ProjectOutputGenerator
import io.tezrok.util.mkdirs
import io.tezrok.util.toPrettyJson
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Entry point for tezrok
 */
class TezrokBuilder private constructor() {
    private var path: Path? = null
    private var output: Path? = null
    private var clock: Clock = Clock.systemDefaultZone()
    private val projectElemRepo = ProjectElemRepository()
    private val projectNodeFactory = ProjectNodeFactory(projectElemRepo)
    private val featureManager = FeatureManager()
    private val generatorProvider = CoreGeneratorProvider()
    private val generator = ProjectOutputGenerator()
    private var outputFinalProject: Boolean = false
    private var finalProjectPath: Path? = null


    fun generate() {
        val inputPath = path ?: throw IllegalStateException("Path not set")
        val projectOutput = output ?: throw IllegalStateException("Output not set")

        if (!projectOutput.exists()) {
            projectOutput.mkdirs()
        }

        val projectElem = projectElemRepo.load(inputPath)
        val project = projectNodeFactory.fromProject(projectElem, projectOutput)
        val context = CoreGeneratorContext(projectElem, generatorProvider, clock)

        if (outputFinalProject) {
            val outputFinalProjectPath = finalProjectPath?.also { it.mkdirs() } ?: projectOutput
            outputFinalProjectPath.resolve("tezrok-final.json").writeText(projectElem.toPrettyJson())
        }

        check(project.getModules().isNotEmpty()) { "No modules found" }

        featureManager.applyAll(project, context)
        generator.generate(project, projectOutput)
    }

    fun setOutput(path: Path): TezrokBuilder {
        output = path
        return this
    }

    fun setOutput(path: String): TezrokBuilder {
        setOutput(Paths.get(path))
        return this
    }

    private fun setPath(path: String): TezrokBuilder {
        setPath(Paths.get(path))
        return this
    }

    private fun setPath(path: Path): TezrokBuilder {
        this.path = path
        return this
    }

    fun setClock(clock: Clock): TezrokBuilder {
        this.clock = clock
        return this
    }

    /**
     * Set to true to output effective project file as json-file
     */
    fun setOutputFinalProject(outputProject: Boolean): TezrokBuilder {
        this.outputFinalProject = outputProject
        return this
    }

    /**
     * Set path to output effective project file
     */
    fun setFinalProjectPath(path: Path): TezrokBuilder {
        this.finalProjectPath = path
        return this
    }

    companion object {
        fun from(path: String) = TezrokBuilder().setPath(path)

        fun from(path: Path) = TezrokBuilder().setPath(path)
    }
}
