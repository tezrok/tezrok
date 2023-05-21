package io.tezrok.spring

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.MavenDependency
import io.tezrok.api.maven.ProjectNode

/**
 * Adds Spring related dependencies and classes.
 */
internal class SpringGenerator : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        check(project.getModules().size == 1) { "SpringGenerator only supports single module" }
        val module = project.getModules().first()
        module.pom.getParentNode().dependencyId = MavenDependency.of("org.springframework.boot:spring-boot-starter-parent:3.1.0")

        return true
    }
}
