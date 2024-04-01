package io.tezrok.core.output

import io.tezrok.api.input.ProjectElem
import io.tezrok.api.maven.ProjectNode
import io.tezrok.core.input.ProjectElemRepository
import java.nio.file.Path

/**
 * Class for creating [ProjectNode]
 */
internal class ProjectNodeFactory(private val projectElemRepository: ProjectElemRepository) {
    fun fromProjectPath(projectPath: Path, projectOutput: Path): ProjectNode {
        return fromProject(projectElemRepository.load(projectPath), projectOutput)
    }

    fun fromProject(projectElem: ProjectElem, projectOutput: Path): ProjectNode {
        val project = ProjectNode(projectElem, projectOutput.normalize().toAbsolutePath())

        projectElem.modules.forEach { module -> project.addModule(module) }

        return project
    }
}
