package io.tezrok.api

import io.tezrok.api.maven.ProjectNode

/**
 * Represents a feature which can be applied to a project
 */
interface TezrokFeature {
    /**
     * Applies this feature to the project
     * @param project the project to apply this feature to
     * @param context the context of the generator
     * @return true if the feature was applied successfully, false otherwise
     */
    fun apply(project: ProjectNode, context: GeneratorContext): Boolean
}
