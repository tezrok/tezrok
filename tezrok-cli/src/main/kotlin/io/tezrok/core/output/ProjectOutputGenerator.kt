package io.tezrok.core.output

import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.maven.ModuleNode
import io.tezrok.api.maven.ProjectNode
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Generates project files from [ProjectNode]
 */
class ProjectOutputGenerator {
    private var deleteOutputDir = false
        set(value) {
            field = value
        }

    @OptIn(ExperimentalPathApi::class)
    fun generate(project: ProjectNode, outputDir: Path) {
        val startTime = System.currentTimeMillis()
        if (deleteOutputDir) {
            log.debug("Deleting output directory: {}", outputDir)
            outputDir.deleteRecursively()
        }
        if (outputDir.notExists()) {
            outputDir.createDirectories()
        }

        for (module in project.getModules()) {
            generateModule(outputDir, module)
        }

        val seconds = (System.currentTimeMillis() - startTime) / 1000
        log.debug("Project generated in {} sec", seconds)
    }

    private fun generateModule(outputDir: Path, module: ModuleNode) {
        log.debug("Generating module: {}", module.getName())
        val moduleOutputDir = outputDir.resolve(module.getName())
        Files.createDirectories(moduleOutputDir)

        val resources = module.getResources()
        moduleOutputDir.resolve(resources.getName())
        Files.createDirectories(moduleOutputDir)
        generateFiles(resources, moduleOutputDir)
    }

    private fun generateFiles(dirNode: BaseFileNode, outputDir: Path) {
        dirNode.getFiles().forEach { file ->
            if (file.isFile()) {
                val outputFile = outputDir.resolve(file.getName())
                outputFile.outputStream().use { writer ->
                    file.getInputStream().use { input ->
                        input.copyTo(writer)
                    }
                }
            } else if (file.isDirectory()) {
                val subDir = outputDir.resolve(file.getName())
                subDir.createDirectories()
                generateFiles(file, subDir)
            } else {
                log.warn("Unknown (not file or dir) file type: {}", file)
            }
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(ProjectOutputGenerator::class.java)!!
    }
}
