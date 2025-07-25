package io.tezrok.sql

import io.tezrok.BaseTest
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.FieldElem
import io.tezrok.api.input.ProjectElem
import io.tezrok.api.input.SchemaElem
import io.tezrok.core.CoreGeneratorContext
import io.tezrok.core.input.ProjectElemRepository
import io.tezrok.json.schema.SchemaLoader
import io.tezrok.util.resourceAsPath
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito

internal class SqlGeneratorTest : BaseTest() {
    private val generatorContext = CoreGeneratorContext(Mockito.mock(ProjectElem::class.java))
    private val schemaLoader = SchemaLoader()
    private val sqlGenerator = CoreSqlGenerator()
    private val projectElemRepository = ProjectElemRepository()

    @Test
    fun testGenerateAsString() {
        val jsonSchema = schemaLoader.load("/schemas/Address.json".resourceAsPath())
        val inheritSchema = SchemaElem(
            entities = listOf(
                EntityElem(
                    name = "Address",
                    fields = listOf(
                        FieldElem(name = "stateId", unique = true),
                        FieldElem(name = "city", uniqueGroup = "CITY_GRP"),
                        FieldElem(name = "zip", uniqueGroup = "CITY_GRP"),
                        FieldElem(name = "street", uniqueGroup = "STREET_UNIQUE")
                    )
                )
            )
        )
        val schema = projectElemRepository.schemaFromJson(jsonSchema, inheritSchema = inheritSchema).copy(schemaName = "tst1")

        assertTrue(schema.entities!!.first().fields.first { it.name == "stateId" }.isUnique())

        val actualSql = sqlGenerator.generate(schema, generatorContext)

        assertResourceEquals("/expected/sql/Address.sql", actualSql.content)
    }

    @Test
    fun testGenerateAsStringWithTwoEntities() {
        val jsonSchema = schemaLoader.load("/schemas/AuthorBooks.json".resourceAsPath())
        val schema = projectElemRepository.schemaFromJson(jsonSchema)
        val actualSql = sqlGenerator.generate(schema, generatorContext)

        assertResourceEquals("/expected/sql/AuthorBooks.sql", actualSql.content)
    }
}
