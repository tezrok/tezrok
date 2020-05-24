package io.tezrok.api.model.node

class ProjectNode(name: String, description: String = "") : Node(name, KIND, description) {
    fun modules(): List<ModuleNode> = children().filterIsInstance<ModuleNode>()

    companion object {
        const val KIND = "Project"
    }
}
