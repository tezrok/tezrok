package io.tezrok.api.model.node

/**
 * Service with methods
 */
class ServiceNode(name: String, parent: ModuleNode, description: String = "") : Node(name, KIND, description, parent) {
    override val parent: ModuleNode
        get() = super.parent as ModuleNode

    companion object {
        const val KIND = "Service"
    }
}
