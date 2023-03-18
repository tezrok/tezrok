package io.tezrok.liquibase

import io.tezrok.core.GeneratorContext
import io.tezrok.core.common.DirectoryNode
import io.tezrok.core.common.FileNode
import io.tezrok.core.output.ProjectNode
import io.tezrok.schema.SchemaLoader
import io.tezrok.util.ResourceUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LiquibaseGeneratorTest {
    private val generatorContext = GeneratorContext()
    private val generator = LiquibaseGenerator(generatorContext)
    private val schemaLoader = SchemaLoader()

    @Test
    fun testGenerate() {
        val project = ProjectNode("TestProject")
        val module = project.addModule("core")
        val jsonSchema = ResourceUtil.getResourceAsPath("/schemas/AddressInfo.json")
        val schema = schemaLoader.load(jsonSchema)
        generator.generate(schema, module)

        val files = module.getResources().getFiles()
        assertEquals(1, files.size)
        val dbDir = files[0]
        assertDirectory(dbDir, "db", 2)
        val dbFiles = dbDir.getFiles()
        assertDirectory(dbFiles[0], "updates", 1)
        assertFile(dbFiles[1], "master.xml")
    }

    private fun assertFile(node: FileNode, name: String) {
        assertEquals(name, node.getName())
        assertTrue(node.isFile()) { "Node should be file: " + node.getName() }
        assertTrue(node.getFilesSize() == 0) { "Node should not have children: " + node.getName() }
    }

    private fun assertDirectory(node: FileNode, name: String, childrenSize: Int) {
        assertEquals(name, node.getName())
        assertTrue(node.isDirectory()) { "Node should be directory: " + node.getName() }
        assertEquals(childrenSize, node.getFilesSize()) { "Node should have $childrenSize children: " + node.getName() }
        assertInstanceOf(
            DirectoryNode::class.java,
            node
        ) { "Node should be instance of DirectoryNode: " + node.getName() }
    }
}
