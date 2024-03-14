package io.tezrok.java

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.ResourceUtil
import org.slf4j.LoggerFactory

/**
 * Adds .gitignore file into root of the project.
 */
internal class GitIgnoreFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val gitignoreFile = project.getOrAddFile(".gitignore")

        if (gitignoreFile.isEmpty()) {
            val lines = ResourceUtil.getResourceAsLines("/templates/java.gitignore.txt").toMutableList()
            context.getProject().git?.let { git ->
                val ignores = git.ignores?.toMutableList() ?: mutableListOf()
                git.excludes?.forEach { line ->
                    lines.remove(line)
                    ignores.remove(line)
                }
                if (ignores.isNotEmpty()) {
                    lines.add("##############################")
                    lines.add("## Custom ignore rules")
                    lines.add("##############################")
                    lines.addAll(ignores)
                }
            }

            gitignoreFile.setString(lines.joinToString("\n"))
        } else {
            log.warn("File .gitignore already exists in the project. Skipping.")
        }

        return true
    }

    private companion object {
        val log = LoggerFactory.getLogger(GitIgnoreFeature::class.java)!!
    }
}
