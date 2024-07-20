package io.tezrok.core.output

import io.tezrok.api.maven.ProjectNode
import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.FileNode
import io.tezrok.api.node.StoreStrategy
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

/**
 * Generates project files from [ProjectNode]
 */
class ProjectOutputGenerator {
    private var deleteOutputDir = false
        set(value) {
            field = value
        }

    fun generate(project: ProjectNode, outputDir: Path) {
        val startTime = System.currentTimeMillis()
        if (deleteOutputDir) {
            log.debug("Deleting output directory: {}", outputDir)
            TODO("Delete only generated files")
        }
        if (outputDir.notExists()) {
            outputDir.createDirectories()
        }

        generateFiles(project, outputDir)

        val seconds = (System.currentTimeMillis() - startTime) / 1000
        log.debug("Project generated in {} sec", seconds)
    }

    private fun generateFiles(dirNode: BaseFileNode, outputDir: Path) {
        val walkedFiles = mutableSetOf<String>()

        dirNode.getFiles().forEach { file ->
            check(!walkedFiles.contains(file.getName())) { "Duplicate file name found: ${file.getName()} in ${dirNode.getName()}" }
            walkedFiles.add(file.getName())

            if (file is FileNode) {
                val outputFile = outputDir.resolve(file.getName())
                val saveFile = file.strategy == StoreStrategy.SAVE
                        || file.strategy == StoreStrategy.SAVE_IF_NOT_EXISTS && outputFile.notExists()
                if (saveFile) {
                    outputFile.outputStream().use { writer ->
                        file.getInputStream().use { input ->
                            input.copyTo(writer)
                        }
                    }
                } else {
                    log.warn("File already exists and strategy is not to overwrite: {}", outputFile)
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
