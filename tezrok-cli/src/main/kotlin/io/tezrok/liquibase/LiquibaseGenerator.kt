package io.tezrok.liquibase

import io.tezrok.core.output.ModuleNode
import io.tezrok.schema.Schema

/**
 * Generates Liquibase changelogs from a JSON schema
 */
class LiquibaseGenerator {
    fun generate(schema: Schema, project: ModuleNode) {
        println("Hello liquibase!")
    }
}
