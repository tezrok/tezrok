package io.tezrok.core.feature

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import io.tezrok.api.maven.UseMavenDependency
import io.tezrok.liquibase.LiquibaseGenerator
import io.tezrok.maven.MavenCoreFeature
import org.apache.commons.lang3.Validate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Manages all [TezrokFeature]s
 */
internal class FeatureManager {
    private val features: MutableList<TezrokFeature> = mutableListOf()

    init {
        // TODO: load features from configuration
        features.add(MavenCoreFeature())
        features.add(LiquibaseGenerator())
    }

    fun applyAll(project: ProjectNode, context: GeneratorContext) {
        features.forEach { feature -> applyFeature(feature, project, context) }
    }

    private fun applyFeature(feature: TezrokFeature, project: ProjectNode, context: GeneratorContext) {
        val success = feature.apply(project, context)

        if (success) {
            log.info("Feature '{}' applied to project '{}'", feature, project.getName())
        } else {
            log.warn("Feature '{}' failed to apply to project '{}'", feature, project.getName())
        }

        if (success && feature.javaClass.isAnnotationPresent(UseMavenDependency::class.java)) {
            // TODO: support multiple modules
            Validate.isTrue(project.getModules().size == 1, "Feature only supports one module")
            val module = project.getModules().first()

            feature.javaClass.annotations
                .filterIsInstance<UseMavenDependency>()
                .map { it.value }
                .forEach { dependency ->
                    if (module.pom.addDependency(dependency)) {
                        log.debug("Automatically added dependency '{}' to module '{}'", dependency, module.getName())
                    }
                }
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(FeatureManager::class.java)
    }
}
