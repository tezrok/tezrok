package io.tezrok.api.model.node

class ProjectNode(name: String, description: String = "") : Node(name, KIND, description, parent = null) {
    fun modules(): List<ModuleNode> = children().filterIsInstance<ModuleNode>()

    fun add(module: ModuleNode): ProjectNode {
        super.add(module)
        return this
    }

    companion object {
        const val KIND = "Project"
    }
}
