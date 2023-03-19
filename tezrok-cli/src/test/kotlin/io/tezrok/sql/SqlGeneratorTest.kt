package io.tezrok.sql

import io.tezrok.schema.SchemaLoader
import io.tezrok.util.resourceAsPath
import io.tezrok.util.resourceAsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlGeneratorTest {
    private val schemaLoader = SchemaLoader()
    private val sqlGenerator = SqlGenerator()

    @Test
    fun testGenerateAsString() {
        val schema = schemaLoader.load("/schemas/Address.json".resourceAsPath())
        val actualSql = sqlGenerator.generateAsString(schema)
        val expectedSql = "/expected/sql/Address.sql".resourceAsString()

        assertEquals(expectedSql, actualSql)
    }

    @Test
    fun testGenerateAsStringWithTwoEntities() {
        val schema = schemaLoader.load("/schemas/AuthorBooks.json".resourceAsPath())
        val actualSql = sqlGenerator.generateAsString(schema)
        val expectedSql = "/expected/sql/AuthorBooks.sql".resourceAsString()

        assertEquals(expectedSql, actualSql)
    }
}
