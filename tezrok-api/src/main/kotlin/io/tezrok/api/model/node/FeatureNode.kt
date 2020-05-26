package io.tezrok.api.model.node

class FeatureNode(name: String, description: String = "") : Node(name, KIND, description) {
    companion object {
        const val KIND = "Feature"
    }
}
