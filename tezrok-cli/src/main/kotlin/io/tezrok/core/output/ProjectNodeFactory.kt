package io.tezrok.core.output

import io.tezrok.api.maven.ModuleNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.core.input.ModuleElem
import io.tezrok.core.input.ProjectElem
import io.tezrok.core.input.ProjectElemRepository
import java.nio.file.Path

/**
 * Class for creating [ProjectNode]
 */
class ProjectNodeFactory(private val projectElemRepository: ProjectElemRepository) {
    fun fromProjectPath(projectPath: Path): ProjectNode {
        return fromProject(projectElemRepository.load(projectPath))
    }

    fun fromProject(projectElem: ProjectElem): ProjectNode {
        val project = ProjectNode(projectElem.name)

        projectElem.modules.map { module -> mapModule(module, project) }

        return project
    }

    private fun mapModule(moduleElem: ModuleElem, project: ProjectNode): ModuleNode {
        val module = project.addModule(moduleElem.name)

        return module
    }
}
