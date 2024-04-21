package io.tezrok.core.feature

import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.ProjectElem
import io.tezrok.api.maven.ProjectNode
import io.tezrok.api.maven.UseMavenDependency
import io.tezrok.db.DataSourceFeature
import io.tezrok.docker.DockerFeature
import io.tezrok.entity.EntityActivableFeature
import io.tezrok.entity.EntityCreatedUpdatedFeature
import io.tezrok.entity.MapperFeature
import io.tezrok.frontend.FrontendFeature
import io.tezrok.java.GitIgnoreFeature
import io.tezrok.java.HelloWorldFeature
import io.tezrok.jooq.EntityGraphLoaderFeature
import io.tezrok.jooq.EntityGraphStoreFeature
import io.tezrok.jooq.JooqEntityCustomMethodsFeature
import io.tezrok.jooq.JooqFeature
import io.tezrok.jooq.JooqRepositoryFeature
import io.tezrok.liquibase.LiquibaseFeature
import io.tezrok.logging.LoggingFeature
import io.tezrok.maven.MavenCoreFeature
import io.tezrok.spring.ControllerFeature
import io.tezrok.spring.ServiceFeature
import io.tezrok.spring.SpringFeature
import io.tezrok.spring.SpringSecurityFeature
import io.tezrok.task.TaskServiceFeature
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Manages all [TezrokFeature]s
 */
internal class FeatureManager {
    private val features: MutableList<TezrokFeature> = mutableListOf()

    init {
        // TODO: load features from configuration
        features.add(EntityActivableFeature())
        features.add(EntityCreatedUpdatedFeature())
        features.add(DataSourceFeature())
        features.add(MavenCoreFeature())
        features.add(MapperFeature())
        features.add(GitIgnoreFeature())
        features.add(HelloWorldFeature())
        features.add(SpringFeature())
        features.add(SpringSecurityFeature())
        features.add(LiquibaseFeature())
        features.add(JooqFeature())
        features.add(JooqRepositoryFeature())
        features.add(EntityGraphLoaderFeature())
        features.add(EntityGraphStoreFeature())
        features.add(JooqEntityCustomMethodsFeature())
        features.add(ServiceFeature())
        features.add(ControllerFeature())
        features.add(DockerFeature())
        features.add(FrontendFeature())
        features.add(TaskServiceFeature())
        features.add(LoggingFeature())
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
            val module = project.getSingleModule()

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

    fun processModel(project: ProjectElem, phase: ProcessModelPhase): ProjectElem {
        var result = project
        for (feature in features) {
            result = feature.processModel(result, phase)
        }

        return result
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(FeatureManager::class.java)
    }
}
