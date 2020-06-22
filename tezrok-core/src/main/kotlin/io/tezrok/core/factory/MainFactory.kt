package io.tezrok.core.factory

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.builder.type.Type
import io.tezrok.api.builder.type.resolver.NamedNodeTypeResolver
import io.tezrok.api.builder.type.resolver.PrimitiveTypeResolver
import io.tezrok.api.model.node.FieldNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.service.CodeService
import io.tezrok.api.service.Service
import io.tezrok.api.visitor.*
import io.tezrok.core.feature.FeatureManager
import io.tezrok.api.error.TezrokException
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.service.Visitor
import io.tezrok.core.generator.*
import io.tezrok.core.service.*
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Exception
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
    private val logicModelVisitors = mutableSetOf<LogicModelVisitor>()

    override fun <T> getInstance(clazz: Class<T>): T {
        val obj = created.computeIfAbsent(clazz) {
            createInstance(clazz).findVisitors()
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
            LogicModelVisitor::class.java -> logicModelVisitors
            else -> throw TezrokException("Type not supported: $clazz")
        } as Set<T>
    }

    override fun <T : Visitor> applyVisitors(clazz: Class<T>, action: (T) -> Unit) {
        val visitors = getServiceList(clazz)

        visitors.forEach { visitor ->
            try {
                log.debug("Begin visitor {}", visitor.javaClass.name)

                action(visitor)

                log.debug("End visitor {}", visitor.javaClass.name)
            } catch (e: Exception) {
                throw TezrokException("Visitor (${visitor.javaClass.name}) failed: ${e.message}", e)
            }
        }
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

    override fun resolveType(field: FieldNode): Type = resolveType(field.type, field.module)

    override fun resolveType(name: String, module: ModuleNode): Type {
        val typeResolver = PrimitiveTypeResolver(NamedNodeTypeResolver(module, module.project))

        return typeResolver.resolveByName(name)
    }

    private fun <T> createInstance(clazz: Class<T>): Any {
        val obj = if (StartUpGenerator::class.java == clazz || FeatureManager::class.java == clazz) {
            clazz.getDeclaredConstructor(Factory::class.java).newInstance(this)!!
        } else if (CodeService::class.java == clazz) {
            throw TezrokException("Class CodeService is context related")
        } else if (Visitor::class.java.isAssignableFrom(clazz) || Generator::class.java.isAssignableFrom(clazz)) {
            clazz.newInstance()!!
        } else {
            throw TezrokException("Unsupported type: $clazz")
        }

        return obj
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
        if (this is LogicModelVisitor) {
            logicModelVisitors.add(this)
        }

        if (this is VisitorsProvider) {
            getVisitors().forEach { it.findVisitors() }
        }

        return this
    }

    companion object {
        private val log = LoggerFactory.getLogger(MainFactory::class.java)

        fun create(project: ProjectNode, targetDir: File): MainFactory {
            return MainFactory(project, targetDir)
        }
    }
}
