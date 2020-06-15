package io.tezrok.core.factory

import io.tezrok.api.ExecuteContext
import io.tezrok.api.builder.type.Type
import io.tezrok.api.builder.type.resolver.NamedNodeTypeResolver
import io.tezrok.api.builder.type.resolver.PrimitiveTypeResolver
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.service.CodeService
import io.tezrok.api.service.Service
import io.tezrok.api.visitor.*
import io.tezrok.core.feature.FeatureManager
import io.tezrok.core.error.TezrokException
import io.tezrok.core.generator.*
import io.tezrok.core.service.*
import java.io.File
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of Factory
 */
class MainFactory(private val project: ProjectNode,
                  private val targetDir: File) : Factory {
    private val created = ConcurrentHashMap<Class<*>, Any>()
    private val mavenVisitors = mutableSetOf<MavenVisitor>()
    private val mainAppVisitors = mutableSetOf<MainAppVisitor>()
    private val eachClassVisitors = mutableSetOf<EachClassVisitor>()
    private val entityClassVisitors = mutableSetOf<EntityClassVisitor>()

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
                LogGenerator::class.java -> LogGenerator()
                EntityGenerator::class.java -> EntityGenerator()
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

    override fun <T : Service> getServiceList(clazz: Class<T>): Set<T> {
        return when (clazz) {
            MavenVisitor::class.java -> mavenVisitors
            MainAppVisitor::class.java -> mainAppVisitors
            EachClassVisitor::class.java -> eachClassVisitors
            EntityClassVisitor::class.java -> entityClassVisitors
            else -> throw TezrokException("Type not supported: $clazz")
        } as Set<T>
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

    override fun resolveType(name: String, context: ExecuteContext): Type {
        val typeResolver = PrimitiveTypeResolver(NamedNodeTypeResolver(context.module, context.project))

        return typeResolver.resolveByName(name)
    }

    companion object {
        fun create(project: ProjectNode, targetDir: File): MainFactory {
            return MainFactory(project, targetDir)
        }
    }

    private fun Any.findVisitors(): Any {
        if (this is MavenVisitor) {
            mavenVisitors.add(this)
        }
        if (this is MainAppVisitor) {
            mainAppVisitors.add(this)
        }
        if (this is EachClassVisitor) {
            eachClassVisitors.add(this)
        }
        if (this is EntityClassVisitor) {
            entityClassVisitors.add(this)
        }

        if (this is VisitorsProvider) {
            getVisitors().forEach { it.findVisitors() }
        }

        return this
    }
}
