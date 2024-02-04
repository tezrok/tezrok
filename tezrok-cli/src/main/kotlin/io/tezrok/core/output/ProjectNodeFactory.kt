package io.tezrok.core.output

import io.tezrok.api.maven.ModuleNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.api.input.ModuleElem
import io.tezrok.api.input.ProjectElem
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
        val project = ProjectNode(projectElem)

        projectElem.modules.map { module -> mapModule(module, project, projectOutput.resolve(module.name)) }

        return project
    }

    private fun mapModule(moduleElem: ModuleElem, project: ProjectNode, physicalPath: Path? = null): ModuleNode {
        val module = project.addModule(moduleElem)
        module.setPhysicalPath(physicalPath?.normalize())
        return module
    }
}
