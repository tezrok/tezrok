package io.tezrok.api.visitor

import io.tezrok.api.GlobalContext
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.service.Visitor

/**
 * Used when need to edit logical model tree
 */
interface LogicModelVisitor : Visitor {
    fun visit(project: ProjectNode, phase: ModelPhase, context: GlobalContext)
}

/**
 * Phases of [LogicModelVisitor]
 */
enum class ModelPhase {
    /**
     * First phase where model can be modified. Called only once
     */
    Init,

    /**
     * Second phase where model can be modified as well. Can be called several times
     */
    Edit,

    /**
     * Last phase where final model can not be modified. Called only once
     */
    PostEdit
}
