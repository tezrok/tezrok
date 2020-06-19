package io.tezrok.api.model.node

class FeatureNode(name: String, parent: ModuleNode, description: String = "") : Node(name, KIND, description, parent) {
    override val parent: ModuleNode
        get() = super.parent as ModuleNode

    companion object {
        const val KIND = "Feature"
    }
}
