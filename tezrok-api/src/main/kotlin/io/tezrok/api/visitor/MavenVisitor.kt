package io.tezrok.api.visitor

import io.tezrok.api.model.maven.Pom
import io.tezrok.api.service.Service

/**
 * Visitor to change maven-related files
 */
interface MavenVisitor : Service {
    /**
     * Called when pom-model initialised
     */
    fun visit(pom: Pom)
}
