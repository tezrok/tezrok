package io.tezrok.api.visitor

import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.service.Visitor

interface LogicModelVisitor : Visitor {
    fun visit(project: ProjectNode, phase: ModelPhase)
}

/**
 * Phases of [LogicModelVisitor]
 */
enum class ModelPhase {
    /**
     * First phase where model can be modified
     */
    Init,

    /**
     * Second phase where model can be modified as well
     */
    Edit,

    /**
     * Last phase where final model can not be modified
     */
    PostEdit
}
