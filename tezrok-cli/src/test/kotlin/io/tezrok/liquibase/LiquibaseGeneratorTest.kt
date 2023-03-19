package io.tezrok.liquibase

import io.tezrok.core.GeneratorContext
import io.tezrok.core.common.DirectoryNode
import io.tezrok.core.common.FileNode
import io.tezrok.core.output.ProjectNode
import io.tezrok.schema.SchemaLoader
import io.tezrok.sql.SqlGenerator
import io.tezrok.util.resourceAsPath
import io.tezrok.util.resourceAsString
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId


class LiquibaseGeneratorTest {
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
        assertFile(updatesDir.getFiles()[0], "2023-03-19_153254-Initial.sql")

        val masterExpected = "/expected/liquibase/master.xml".resourceAsString()
        assertEquals(masterExpected, masterFile.asText())
        val sqlExpected = "/expected/liquibase/initial_script.sql".resourceAsString()
        assertEquals(sqlExpected, updatesDir.getFiles()[0].asText())
    }

    private fun assertFile(node: FileNode, name: String): FileNode {
        assertEquals(name, node.getName())
        assertTrue(node.isFile()) { "Node should be file: " + node.getName() }
        assertTrue(node.getFilesSize() == 0) { "Node should not have children: " + node.getName() }
        return node
    }

    private fun assertDirectory(node: FileNode, name: String, childrenSize: Int): DirectoryNode {
        assertEquals(name, node.getName())
        assertTrue(node.isDirectory()) { "Node should be directory: " + node.getName() }
        assertEquals(childrenSize, node.getFilesSize()) { "Node should have $childrenSize children: " + node.getName() }
        assertInstanceOf(
            DirectoryNode::class.java,
            node
        ) { "Node should be instance of DirectoryNode: " + node.getName() }

        return node as DirectoryNode
    }
}
