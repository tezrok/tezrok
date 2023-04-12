package io.tezrok.api

import io.tezrok.api.maven.ProjectNode

/**
 * Represents a feature which can be applied to a project
 */
interface TezrokFeature {
    fun apply(project: ProjectNode, context: GeneratorContext)
}
