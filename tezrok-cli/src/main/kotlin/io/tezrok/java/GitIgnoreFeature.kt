package io.tezrok.java

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.ResourceUtil

/**
 * Adds .gitignore file into root of the project.
 */
internal class GitIgnoreFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val gitignoreFile = project.getOrAddFile(".gitignore")

        if (gitignoreFile.isEmpty()) {
            gitignoreFile.setString(ResourceUtil.getResourceAsString("/templates/java.gitignore.txt"))
        }

        return true
    }
}
