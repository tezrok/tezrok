package io.tezrok.api.maven

import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.BaseNode
import io.tezrok.api.node.DirectoryNode
import java.nio.file.Path

/**
 * Represents a module. Which represents a separate maven module
 * which can be a library or a web application
 */
open class ModuleNode(name: String, parent: BaseNode?, private var physicalPath: Path? = null) : DirectoryNode(name, parent) {
    val source: SourceNode = SourceNode(this)

    val pom: PomNode = PomNode(artifactId = name, parent = this)

    override fun getPhysicalPath(): Path? = physicalPath

    fun setPhysicalPath(physicalPath: Path?) {
        this.physicalPath = physicalPath
    }

    override fun getFiles(): List<BaseFileNode> = listOf(source, pom)
}
