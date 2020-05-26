package io.tezrok.api.model.node

class ModuleNode(name: String, description: String = "") : Node(name, KIND, description) {
    /**
     * Used features in the module
     */
    fun features(): List<FeatureNode> = children().filterIsInstance<FeatureNode>()

    fun entities(): List<EntityNode> = children().filterIsInstance<EntityNode>()

    fun services(): List<ServiceNode> = children().filterIsInstance<ServiceNode>()

    fun enums(): List<EnumNode> = children().filterIsInstance<EnumNode>()

    fun use(feature: FeatureNode): ModuleNode {
        add(feature)
        return this
    }

    companion object {
        const val KIND = "Module"
    }
}
