package io.tezrok.core

import io.tezrok.core.feature.FeatureManager
import io.tezrok.core.input.ProjectElemRepository
import io.tezrok.core.output.ProjectNodeFactory
import io.tezrok.core.output.ProjectOutputGenerator
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Entry point for tezrok
 */
class TezrokBuilder private constructor() {
    private var path: Path? = null
    private var output: Path? = null
    private val projectElemRepo = ProjectElemRepository()
    private val projectNodeFactory = ProjectNodeFactory(projectElemRepo)
    private val featureManager = FeatureManager()
    private val generatorProvider = CoreGeneratorProvider()
    private val generator = ProjectOutputGenerator()

    fun generate() {
        val inputPath = path ?: throw IllegalStateException("Path not set")
        val projectOutput = output ?: throw IllegalStateException("Output not set")

        val projectElem = projectElemRepo.load(inputPath)
        val project = projectNodeFactory.fromProject(projectElem)
        val context = CoreGeneratorContext(projectElem, generatorProvider)

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

    companion object {
        fun from(path: String) = TezrokBuilder().setPath(path)

        fun from(path: Path) = TezrokBuilder().setPath(path)
    }
}
