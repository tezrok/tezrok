package io.tezrok.sql

import io.tezrok.BaseTest
import io.tezrok.schema.SchemaLoader
import io.tezrok.util.resourceAsPath
import org.junit.jupiter.api.Test

internal class SqlGeneratorTest : BaseTest() {
    private val schemaLoader = SchemaLoader()
    private val sqlGenerator = SqlGenerator()

    @Test
    fun testGenerateAsString() {
        val schema = schemaLoader.load("/schemas/Address.json".resourceAsPath())
        val actualSql = sqlGenerator.generateAsString(schema)

        assertResourceEquals("/expected/sql/Address.sql", actualSql)
    }

    @Test
    fun testGenerateAsStringWithTwoEntities() {
        val schema = schemaLoader.load("/schemas/AuthorBooks.json".resourceAsPath())
        val actualSql = sqlGenerator.generateAsString(schema)

        assertResourceEquals("/expected/sql/AuthorBooks.sql", actualSql)
    }
}
