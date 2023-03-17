package io.tezrok.liquibase

import io.tezrok.core.output.ProjectNode
import io.tezrok.schema.Schema

/**
 * Generates Liquibase changelogs from a JSON schema
 */
class LiquibaseGenerator {
    fun generate(schema: Schema, project: ProjectNode) {
        println("Hello liquibase!")
    }
}
