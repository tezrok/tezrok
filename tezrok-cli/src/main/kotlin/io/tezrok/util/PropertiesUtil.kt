package io.tezrok.util

import io.tezrok.api.input.ModuleElem
import io.tezrok.api.node.FileNode
import io.tezrok.util.PathUtil.NEW_LINE

fun FileNode.addNewSettings(moduleElem: ModuleElem?, vararg newLines: String) {
    val lines = this.asLines().toMutableSet()
    lines.addAll(newLines)

    moduleElem?.spring?.properties?.let { props ->
        props.includes?.filter { key -> !lines.contains(key) }?.forEach(lines::add)
        props.excludes?.forEach(lines::remove)
    }

    this.setString(lines.joinToString(NEW_LINE) + NEW_LINE)
}
