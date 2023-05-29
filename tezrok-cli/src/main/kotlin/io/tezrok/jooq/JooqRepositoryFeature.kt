package io.tezrok.jooq

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

internal class JooqRepositoryFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val javaRoot = module.source.main.java
        val applicationPackageRoot = javaRoot.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val repositoryDir = applicationPackageRoot.getOrAddJavaDirectory("repository")
            val baseRepoClass = repositoryDir.getOrAddClass("BaseRepository")

            TODO("Add jooq dependency to pom.xml")
        }

        return true
    }
}
