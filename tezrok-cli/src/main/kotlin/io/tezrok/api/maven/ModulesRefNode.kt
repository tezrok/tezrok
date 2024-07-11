package io.tezrok.api.maven

import io.tezrok.api.xml.XmlNode
import java.util.*
import java.util.function.Function
import java.util.stream.Stream

/**
 * Part of the pom.xml file that contains information about all modules.
 */
open class ModulesRefNode(private val node: XmlNode) {
    fun addModule(name: String) {
        if (!hasModule(name)) {
            node.add("module", name)
        }
    }

    fun removeModule(name: String): Boolean {
        return node.remove(getModuleByName(name)
            .map { listOf(it) }
            .orElseGet(::emptyList))
    }

    /**
     * Sorts modules by the given selector.
     */
    fun sortModules(selector: Function<String, Int>) {
        setModules(getModules().sortedBy { node -> selector.apply(node.getValue()!!) })
    }

    fun getModules(): List<XmlNode> = moduleNodes().toList()

    fun setModules(modules: List<XmlNode>) {
        node.remove(moduleNodes().toList())
        modules.forEach { node.add(it) }
    }

    fun hasModule(name: String) = getModuleByName(name).isPresent

    private fun getModuleByName(name: String): Optional<XmlNode> = moduleNodes()
        .filter { !it.hasChildren() && it.getValue() == name }
        .findFirst()

    private fun moduleNodes(): Stream<XmlNode> = node.nodesByPath("module")
}
