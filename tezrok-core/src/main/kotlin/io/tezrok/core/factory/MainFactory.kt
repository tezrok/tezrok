package io.tezrok.core.factory

import io.tezrok.api.Generator
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.visitor.MavenVisitor
import io.tezrok.core.MavenVisitorsProvider
import io.tezrok.core.feature.FeatureManager
import io.tezrok.core.error.TezrokException
import io.tezrok.core.generator.CoreGenerator
import io.tezrok.core.generator.HelloWorldGenerator
import io.tezrok.core.generator.MavenGenerator
import io.tezrok.core.generator.StartUpGenerator
import java.io.File
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of Factory
 */
class MainFactory(private val project: ProjectNode,
                  private val targetDir: File) : Factory {
    val context = MainExecuteContext(project, targetDir, this)
    private val created = ConcurrentHashMap<Class<*>, Any>()
    private val mavenVisitors = mutableListOf<MavenVisitor>()

    override fun <T> getInstance(clazz: Class<T>): T {
        val obj = created.computeIfAbsent(clazz) {
            when (clazz) {
                CoreGenerator::class.java -> CoreGenerator()
                StartUpGenerator::class.java -> StartUpGenerator()
                FeatureManager::class.java -> FeatureManager(this)
                HelloWorldGenerator::class.java -> HelloWorldGenerator()
                MavenGenerator::class.java -> MavenGenerator()
                MavenVisitorsProvider::class.java -> MavenVisitorsProvider(mavenVisitors)
                else -> throw TezrokException("Unsupported type: $clazz")
            }.also {
                if (it is MavenVisitor) {
                    mavenVisitors.add(it)
                }
            }
        }

        return obj as T
    }

    override fun getGenerator(className: String): Generator {
        val clazz = Class.forName(className)

        if (Generator::class.java.isAssignableFrom(clazz)) {
            return getInstance(clazz as Class<Generator>)
        }

        throw IllegalStateException("Class name is not generator: $className")
    }

    companion object {
        fun create(project: ProjectNode, targetDir: File): MainFactory {
            return MainFactory(project, targetDir)
        }
    }
}
