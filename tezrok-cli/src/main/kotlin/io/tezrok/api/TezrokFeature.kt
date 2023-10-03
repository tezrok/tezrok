package io.tezrok.api

import io.tezrok.api.input.ProjectElem
import io.tezrok.api.maven.ProjectNode

/**
 * Represents a feature which can be applied to a project.
 */
interface TezrokFeature {
    /**
     * Applies this feature to the project
     *
     * @param project the project to apply this feature to
     * @param context the context of the generator
     * @return true if the feature was applied successfully, false otherwise
     */
    fun apply(project: ProjectNode, context: GeneratorContext): Boolean

    /**
     * Method provides a way to modify the model of the project before applying the features.
     *
     * @param project the model of the project
     * @return the modified model of the project
     */
    fun processModel(project: ProjectElem): ProjectElem {
        return project
    }
}
