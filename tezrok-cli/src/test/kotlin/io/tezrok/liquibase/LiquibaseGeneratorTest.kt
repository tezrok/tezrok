package io.tezrok.liquibase

import io.tezrok.BaseTest
import io.tezrok.core.GeneratorContext
import io.tezrok.core.output.ProjectNode
import io.tezrok.schema.SchemaLoader
import io.tezrok.sql.SqlGenerator
import io.tezrok.util.resourceAsPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId


internal class LiquibaseGeneratorTest : BaseTest() {
    private val fixedClock = Clock.fixed(Instant.parse("2023-03-19T14:32:54.00Z"), ZoneId.systemDefault())
    private val generatorContext = GeneratorContext()
    private val sqlGenerator = SqlGenerator()
    private val generator = LiquibaseGenerator(generatorContext, sqlGenerator, fixedClock)
    private val schemaLoader = SchemaLoader()

    @Test
    fun testGenerate() {
        val project = ProjectNode("TestProject")
        val module = project.addModule("core")
        val schema = schemaLoader.load("/schemas/Address.json".resourceAsPath())
        generator.generate(schema, module)

        val files = module.getResources().getFiles()
        assertEquals(1, files.size)
        val dbDir = files[0]
        assertDirectory(dbDir, "db", 2)
        val dbFiles = dbDir.getFiles()
        val updatesDir = assertDirectory(dbFiles[0], "updates", 1)
        val masterFile = assertFile(dbFiles[1], "master.xml")
        val initialSqlFile = updatesDir.getFiles()[0]
        assertFile(initialSqlFile, "2023-03-19_153254-Initial.sql")

        assertResourceEquals("/expected/liquibase/master.xml", masterFile.asString())
        assertResourceEquals("/expected/liquibase/initial_script.sql", initialSqlFile.asString())
    }
}
