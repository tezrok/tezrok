package io.tezrok.liquibase

import io.tezrok.BaseTest
import io.tezrok.api.input.ModuleElem
import io.tezrok.api.input.ProjectElem
import io.tezrok.api.maven.ProjectNode
import io.tezrok.core.CoreGeneratorContext
import io.tezrok.core.CoreGeneratorProvider
import io.tezrok.core.input.ProjectElemRepository
import io.tezrok.json.schema.Schema
import io.tezrok.json.schema.SchemaLoader
import io.tezrok.util.resourceAsPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class LiquibaseFeatureTest : BaseTest() {
    private val fixedClock = getFixedClock()
    private val liquibaseFeature = LiquibaseFeature()
    private val schemaLoader = SchemaLoader()
    private val projectElemRepository = ProjectElemRepository()

    @Test
    fun testGenerate() {
        val schema = schemaLoader.load("/schemas/Address.json".resourceAsPath())
        val projectInput = mockProjectInput("core", schema)
        val generatorProvider = CoreGeneratorProvider()
        val generatorContext = CoreGeneratorContext(projectInput, generatorProvider, fixedClock)
        val project = ProjectNode(projectInput)
        val module = project.addModule(projectInput.modules[0])
        module.properties.setProperty("datasource.username", "TestTezrokUser")
        liquibaseFeature.apply(project, generatorContext)
        val files = module.source.main.resources.getFiles()
        assertEquals(2, files.size) { "Expected 1 file, but was: $files" }
        assertFile(files[0], "application.properties")
        val dbDir = assertDirectory(files[1], "db", 2)
        val dbFiles = dbDir.getFiles()
        val updatesDir = assertDirectory(dbFiles[0], "updates", 1)
        val masterFile = assertFile(dbFiles[1], "master.xml")
        val initialSqlFile = updatesDir.getFiles()[0]
        assertFile(initialSqlFile, "2023-03-19_153254-Initial.sql")

        assertResourceEquals("/expected/liquibase/master.xml", masterFile.asString())
        assertResourceEquals("/expected/liquibase/initial_script.sql", initialSqlFile.asString())
    }

    private fun mockProjectInput(moduleName: String, schema: Schema): ProjectElem {
        val moduleInput = ModuleElem(name = moduleName, schema = projectElemRepository.schemaFromJson(schema))
        return ProjectElem(modules = listOf(moduleInput))
    }
}
