package io.tezrok.api.visitor

import io.tezrok.api.model.maven.Pom

/**
 * Visitor to change pom-file's content
 */
interface MavenVisitor {
    /**
     * Called when pom-model initialised
     */
    fun visit(pom: Pom)
}
