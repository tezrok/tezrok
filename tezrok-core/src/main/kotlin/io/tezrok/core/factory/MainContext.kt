package io.tezrok.core.factory

import io.tezrok.api.GeneratorContext
import io.tezrok.api.builder.type.Type
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode

class MainContext(private val project: ProjectNode,
                  private val factory: Factory) : GeneratorContext {
    override fun isGenerateTime(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getProject(): ProjectNode = project

    override fun ofType(clazz: Class<*>?): Type {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getModule(type: Type?): ModuleNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T> getInstance(clazz: Class<T>): T {
        return factory.getInstance(clazz)
    }
}
