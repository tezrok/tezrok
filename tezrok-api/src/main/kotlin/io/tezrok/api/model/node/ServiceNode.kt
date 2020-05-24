package io.tezrok.api.model.node

/**
 * Service with methods
 */
class ServiceNode(name: String, description: String = "") : Node(name, KIND, description) {
    companion object {
        const val KIND = "Service"
    }
}
