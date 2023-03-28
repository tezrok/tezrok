package io.tezrok.liquibase

import io.tezrok.BaseTest
import io.tezrok.api.node.ProjectNode
import io.tezrok.core.CoreGeneratorContext
import io.tezrok.schema.SchemaLoader
import io.tezrok.sql.CoreSqlGenerator
import io.tezrok.util.resourceAsPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class LiquibaseGeneratorTest : BaseTest() {
    private val fixedClock = getFixedClock("2023-03-19T14:32:54.00Z")
    private val generatorContext = CoreGeneratorContext(fixedClock)
    private val sqlGenerator = CoreSqlGenerator()
    private val liquibaseFeature = LiquibaseGenerator(sqlGenerator)
    private val schemaLoader = SchemaLoader()

    @Test
    fun testGenerate() {
        val project = ProjectNode("TestProject")
        val module = project.addModule("core")
        val schema = schemaLoader.load("/schemas/Address.json".resourceAsPath())
        module.schema = schema
        liquibaseFeature.apply(project, generatorContext)

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
