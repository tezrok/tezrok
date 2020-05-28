package io.tezrok.core.factory

import io.tezrok.api.GeneratorContext
import io.tezrok.api.builder.Builder
import io.tezrok.api.builder.type.Type
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.core.generator.toMavenVersion
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter

class MainContext(private val project: ProjectNode,
                  private val targetDir: File,
                  private val factory: Factory) : GeneratorContext {
    private val log = LoggerFactory.getLogger(MainContext::class.java)
    private val moduleRootDir = File(targetDir, module.toMavenVersion().artifactId)

    override fun isGenerateTime(): Boolean = true

    override fun getProject(): ProjectNode = project

    override fun ofType(clazz: Class<*>): Type {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getModule(type: Type): ModuleNode = module

    override fun <T> getInstance(clazz: Class<T>): T {
        return factory.getInstance(clazz)
    }

    override fun getModule(): ModuleNode {
        // TODO: make selectable module
        return project.modules().first()
    }

    override fun render(builder: Builder) {
        val targetDir = File(moduleRootDir, builder.path.replace('.', '/'))
        val targetFile = File(targetDir, builder.fileName)

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        if (!targetFile.exists()) {
            log.debug("Generating {}", targetFile)
            val fw = FileWriter(targetFile)
            builder.build(fw)
            fw.close()
        } else {
            log.warn("Skip generating {}, already exists", targetFile)
        }
    }
}
