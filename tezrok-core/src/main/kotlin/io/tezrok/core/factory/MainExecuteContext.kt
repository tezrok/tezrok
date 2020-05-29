package io.tezrok.core.factory

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Phase
import io.tezrok.api.builder.Builder
import io.tezrok.api.builder.type.Type
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.core.error.TezrokException
import org.slf4j.LoggerFactory
import java.io.File

open class MainExecuteContext(private val project: ProjectNode,
                              private val targetDir: File) : ExecuteContext {


    private val log = LoggerFactory.getLogger(javaClass)

    override fun getProject(): ProjectNode = project

    fun getTargetDir(): File = targetDir

    override fun getPhase(): Phase {
        throw TezrokException("Subclass must implement this method")
    }

    override fun getModule(): ModuleNode {
        throw TezrokException("Subclass must implement this method")
    }

    override fun render(builder: Builder) {
        throw TezrokException("Subclass must implement this method")
    }

    override fun ofType(clazz: Class<*>): Type {
        throw TezrokException("Subclass must implement this method")
    }

    override fun ofType(name: String): Type {
        throw TezrokException("Subclass must implement this method")
    }

    override fun <T> getInstance(clazz: Class<T>): T {
        throw TezrokException("Subclass must implement this method")
    }

    override fun isGenerateTime(): Boolean = false
}
