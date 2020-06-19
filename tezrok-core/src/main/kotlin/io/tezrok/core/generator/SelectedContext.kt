package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Phase
import io.tezrok.api.builder.Builder
import io.tezrok.api.builder.type.NamedType
import io.tezrok.api.builder.type.Type
import io.tezrok.api.model.node.FieldNode
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.service.Service
import io.tezrok.api.service.Visitor
import io.tezrok.core.factory.Factory
import io.tezrok.core.util.PackageUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter

/**
 * Implementation of {@link ExecuteContext} when Phase and Module changed
 */
internal class SelectedContext(val selectedPhase: Phase,
                               val selectedModule: ModuleNode,
                               val factory: Factory) : ExecuteContext {
    override val phase: Phase = selectedPhase

    override val module: ModuleNode = selectedModule

    override val project: ProjectNode = factory.getProject()

    override val generateTime: Boolean = true

    override val overwriteIfExists: Boolean = true

    override fun <T> getInstance(clazz: Class<T>): T = factory.getInstance(clazz, this)

    override fun <T : Service> getServiceList(clazz: Class<T>): Set<T> = factory.getServiceList(clazz)

    override fun render(builder: Builder) {
        if (phase != Phase.Generate) {
            return
        }

        // TODO: rewrite getting module's dir name
        val moduleRootDir = File(factory.getTargetDir(), module.toMavenVersion().artifactId)
        val targetDir = File(moduleRootDir, builder.path.replace('.', '/'))
        val targetFile = File(targetDir, builder.fileName)

        if (!targetDir.exists()) {
            val success = targetDir.mkdirs()
            log.debug("Create dir {}, success: {}", targetDir, success)
        }

        if (!targetFile.exists() || overwriteIfExists) {
            log.debug("Generating {}", targetFile)
            val fw = FileWriter(targetFile)
            builder.build(fw)
            fw.close()
        } else {
            log.warn("Skip generating {}, already exists", targetFile)
        }
    }

    override fun <T : Visitor> applyVisitors(clazz: Class<T>, action: (T) -> Unit) = factory.applyVisitors(clazz, action)

    override fun ofType(name: String, subPath: String): Type {
        return NamedType(name, PackageUtil.concat(module.packagePath, subPath))
    }

    override fun ofType(clazz: Class<*>): Type {
        return NamedType(clazz)
    }

    override fun resolveType(name: String): Type = factory.resolveType(name, module)

    override fun resolveType(field: FieldNode): Type = factory.resolveType(field)

    companion object {
        private val log = LoggerFactory.getLogger(javaClass)
    }
}
