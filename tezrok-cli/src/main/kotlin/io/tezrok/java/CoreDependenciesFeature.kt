package io.tezrok.java

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

/**
 * Adds core dependencies to the project.
 */
open class CoreDependenciesFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val pom = project.getSingleModule().pom
        pom.addDependency("org.jetbrains:annotations:24.0.1")

        return true
    }
}
