package io.tezrok.api.model.node

class ModuleNode(name: String,
                 val packagePath: String,
                 val version: String,
                 description: String = "") : Node(name, KIND, description) {
    /**
     * Used features in the module
     */
    fun features(): List<FeatureNode> = children().filterIsInstance<FeatureNode>()

    fun entities(): List<EntityNode> = children().filterIsInstance<EntityNode>()

    fun services(): List<ServiceNode> = children().filterIsInstance<ServiceNode>()

    fun enums(): List<EnumNode> = children().filterIsInstance<EnumNode>()

    fun use(feature: FeatureNode): ModuleNode {
        super.add(feature)
        return this
    }

    fun add(entity: EntityNode): ModuleNode {
        super.add(entity)
        return this
    }

    fun add(service: ServiceNode): ModuleNode {
        super.add(service)
        return this
    }

    fun add(enum: EnumNode): ModuleNode {
        super.add(enum)
        return this
    }

    companion object {
        const val KIND = "Module"
    }
}
