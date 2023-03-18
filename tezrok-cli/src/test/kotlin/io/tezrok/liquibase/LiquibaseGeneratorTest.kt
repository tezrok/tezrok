package io.tezrok.liquibase

import io.tezrok.core.output.ProjectNode
import io.tezrok.schema.SchemaLoader
import io.tezrok.util.ResourceUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LiquibaseGeneratorTest {
    private val generator = LiquibaseGenerator()
    private val schemaLoader = SchemaLoader()

    @Test
    fun test() {
        val project = ProjectNode("TestProject")
        val module = project.addModule("core")
        val jsonSchema = ResourceUtil.getResourceAsPath("/schemas/AddressInfo.json")
        val schema = schemaLoader.load(jsonSchema)
        generator.generate(schema, module)

        assertEquals("AddressInfo", project.getModules()[0].getName())
    }
}
