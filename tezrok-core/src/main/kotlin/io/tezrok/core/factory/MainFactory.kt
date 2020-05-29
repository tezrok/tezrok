package io.tezrok.core.factory

import io.tezrok.api.ExecuteContext
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.service.CodeService
import io.tezrok.api.service.Service
import io.tezrok.api.visitor.MainAppVisitor
import io.tezrok.api.visitor.MavenVisitor
import io.tezrok.core.service.MavenVisitorsProvider
import io.tezrok.core.feature.FeatureManager
import io.tezrok.core.error.TezrokException
import io.tezrok.core.generator.CoreGenerator
import io.tezrok.core.generator.MavenGenerator
import io.tezrok.core.generator.StartUpGenerator
import io.tezrok.core.service.CodeServiceImpl
import io.tezrok.core.service.MainAppVisitorsProvider
import java.io.File
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of Factory
 */
class MainFactory(private val project: ProjectNode,
                  private val targetDir: File) : Factory {
    private val created = ConcurrentHashMap<Class<*>, Any>()
    private val mavenVisitors = mutableListOf<MavenVisitor>()
    private val mainAppVisitors = mutableListOf<MainAppVisitor>()

    override fun <T> getInstance(clazz: Class<T>): T {
        if (!created.contains(clazz) && (MavenVisitor::class.java.isAssignableFrom(clazz)
                        || MainAppVisitor::class.java.isAssignableFrom(clazz))) {
            created[clazz] = clazz.newInstance()!!.findVisitors()
        }

        val obj = created.computeIfAbsent(clazz) {
            when (clazz) {
                CoreGenerator::class.java -> CoreGenerator()
                StartUpGenerator::class.java -> StartUpGenerator(this)
                FeatureManager::class.java -> FeatureManager(this)
                MavenGenerator::class.java -> MavenGenerator()
                MavenVisitorsProvider::class.java -> MavenVisitorsProvider(mavenVisitors)
                MainAppVisitorsProvider::class.java -> MainAppVisitorsProvider(mainAppVisitors)
                CodeService::class.java -> throw TezrokException("Class CodeService is context related")
                else -> throw TezrokException("Unsupported type: $clazz")
            }.findVisitors()
        }

        return obj as T
    }

    override fun <T> getInstance(clazz: Class<T>, context: ExecuteContext): T {
        val obj = when (clazz) {
            CodeService::class.java -> CodeServiceImpl(context)
            else -> getInstance(clazz)
        }

        return obj as T
    }

    override fun createService(className: String): Service {
        val clazz = Class.forName(className)

        if (Service::class.java.isAssignableFrom(clazz)) {
            return getInstance(clazz as Class<Service>)
        }

        throw IllegalStateException("Class name is not a service: $className")
    }

    override fun getProject(): ProjectNode = project

    override fun getTargetDir(): File = targetDir

    companion object {
        fun create(project: ProjectNode, targetDir: File): MainFactory {
            return MainFactory(project, targetDir)
        }
    }

    private fun Any.findVisitors(): Any {
        when (this) {
            is MavenVisitor -> mavenVisitors.add(this)
            is MainAppVisitor -> mainAppVisitors.add(this)
        }

        return this
    }
}
