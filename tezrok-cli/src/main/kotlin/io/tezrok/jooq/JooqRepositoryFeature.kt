package io.tezrok.jooq

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import org.slf4j.LoggerFactory

/**
 * Creates repository class for each entity.
 */
internal class JooqRepositoryFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val javaRoot = module.source.main.java
        val applicationPackageRoot = javaRoot.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val repositoryDir = applicationPackageRoot.getOrAddJavaDirectory("repository")
            val jooqRepoFile = repositoryDir.getOrAddFile("JooqRepository.java")

            if (jooqRepoFile.isEmpty()) {
                context.writeTemplate(jooqRepoFile, "/templates/liquibase/JooqRepository.java.vm") { velContext ->
                    velContext.put("package", context.getProject().packagePath + ".repository")
                }
            } else {
                log.warn("File ${jooqRepoFile.getName()} already exists")
            }
        } else {
            log.warn("Application package root is not set")
        }

        return true
    }

    private companion object {
        val log = LoggerFactory.getLogger(JooqRepositoryFeature::class.java)!!
    }
}
