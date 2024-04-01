package io.tezrok.api.maven

import io.tezrok.api.TezrokProperties
import io.tezrok.api.input.ModuleElem
import io.tezrok.api.input.ModuleType
import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.BaseNode
import io.tezrok.api.node.DirectoryNode

/**
 * Represents a module. Which represents a separate maven module
 * which can be a library or a web application
 */
open class ModuleNode(moduleElem: ModuleElem, parent: BaseNode?) : DirectoryNode(moduleElem.name, parent) {
    val source: SourceNode = SourceNode(this)

    val pom: PomNode = PomNode(artifactId = moduleElem.name, parent = this)

    val properties: TezrokProperties = PropertiesNode(moduleElem)

    val custom: Boolean = moduleElem.type == ModuleType.Custom

    init {
        if (!custom && moduleElem.description.isNotBlank()) {
            pom.setDescription(moduleElem.description)
        }
    }

    override fun getFiles(): List<BaseFileNode> = if (custom) emptyList() else listOf(source, pom)
}
