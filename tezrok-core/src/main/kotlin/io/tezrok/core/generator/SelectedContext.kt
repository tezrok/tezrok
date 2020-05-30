package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Phase
import io.tezrok.api.builder.Builder
import io.tezrok.api.builder.type.NamedType
import io.tezrok.api.builder.type.Type
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.core.factory.Factory
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter

internal class SelectedContext(val selectedPhase: Phase,
                               val selectedModule: ModuleNode,
                               val factory: Factory) : ExecuteContext {
    private val log = LoggerFactory.getLogger(javaClass)

    override val phase: Phase = selectedPhase

    override val module: ModuleNode = selectedModule

    override val project: ProjectNode = factory.getProject()

    override val generateTime: Boolean = true

    override val overwriteIfExists: Boolean = true

    override fun <T> getInstance(clazz: Class<T>): T = factory.getInstance(clazz, this)

    override fun render(builder: Builder) {
        if (phase != Phase.Generate) {
            return
        }

        val moduleRootDir = File(factory.getTargetDir(), module.toMavenVersion().artifactId)
        val targetDir = File(moduleRootDir, builder.path.replace('.', '/'))
        val targetFile = File(targetDir, builder.fileName)

        if (!targetDir.exists()) {
            targetDir.mkdirs()
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

    override fun ofType(name: String): Type {
        return NamedType(name, module.packagePath)
    }

    override fun ofType(clazz: Class<*>): Type {
        return NamedType(clazz)
    }
}
