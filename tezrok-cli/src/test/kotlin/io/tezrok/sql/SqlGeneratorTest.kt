package io.tezrok.sql

import io.tezrok.BaseTest
import io.tezrok.core.CoreGeneratorContext
import io.tezrok.api.input.ProjectElem
import io.tezrok.schema.SchemaLoader
import io.tezrok.util.resourceAsPath
import org.junit.jupiter.api.Test
import org.mockito.Mockito

internal class SqlGeneratorTest : BaseTest() {
    private val generatorContext = CoreGeneratorContext(Mockito.mock(ProjectElem::class.java))
    private val schemaLoader = SchemaLoader()
    private val sqlGenerator = CoreSqlGenerator()

    @Test
    fun testGenerateAsString() {
        val schema = schemaLoader.load("/schemas/Address.json".resourceAsPath())
        val actualSql = sqlGenerator.generate(schema, generatorContext)

        assertResourceEquals("/expected/sql/Address.sql", actualSql.content)
    }

    @Test
    fun testGenerateAsStringWithTwoEntities() {
        val schema = schemaLoader.load("/schemas/AuthorBooks.json".resourceAsPath())
        val actualSql = sqlGenerator.generate(schema, generatorContext)

        assertResourceEquals("/expected/sql/AuthorBooks.sql", actualSql.content)
    }
}
