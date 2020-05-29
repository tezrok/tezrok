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

    override fun getPhase(): Phase = selectedPhase

    override fun getModule(): ModuleNode = selectedModule

    override fun <T> getInstance(clazz: Class<T>): T = factory.getInstance(clazz, this)

    override fun getProject(): ProjectNode = factory.getProject()

    override fun isGenerateTime(): Boolean = true

    override fun overwriteIfExists(): Boolean = true

    override fun render(builder: Builder) {
        if (getPhase() != Phase.Generate) {
            return
        }

        val moduleRootDir = File(factory.getTargetDir(), getModule().toMavenVersion().artifactId)
        val targetDir = File(moduleRootDir, builder.path.replace('.', '/'))
        val targetFile = File(targetDir, builder.fileName)

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        if (!targetFile.exists() || overwriteIfExists()) {
            log.debug("Generating {}", targetFile)
            val fw = FileWriter(targetFile)
            builder.build(fw)
            fw.close()
        } else {
            log.warn("Skip generating {}, already exists", targetFile)
        }
    }

    override fun ofType(name: String): Type {
        return NamedType(name, getModule().packagePath)
    }

    override fun ofType(clazz: Class<*>): Type {
        return NamedType(clazz)
    }
}
