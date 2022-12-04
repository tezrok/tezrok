package com.tezrok.api.tree

import com.tezrok.BaseTest
import com.tezrok.api.tree.repo.file.FileNodeElem
import com.tezrok.api.tree.repo.file.FileNodeRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class NodeManagerTest : BaseTest() {

    @Test
    fun getRootNodeWhenFileNotExists() {
        val file = File(tempDir, UUID.randomUUID().toString())
        val repository = FileNodeRepository(file)
        val manager = NodeManagerImpl(repository)
        val root = manager.getRootNode()

        assertEquals(FileNodeElem.ROOT_NAME, root.getName())
        assertEquals(1000L, root.getId())
        assertEquals(0, root.getChildrenSize())
        assertEmpty(root.getChildren().toList())

        val properties = root.getProperties()
        assertEmpty(properties.getPropertiesNames())
        assertFalse(file.exists()) { "File must not exist: $file" }
    }

    @Test
    fun saveSingleRootTest() {
        val file = File(tempDir, UUID.randomUUID().toString())

        try {
            val repository = FileNodeRepository(file)
            val manager = NodeManagerImpl(repository)
            val root = manager.getRootNode()
            repository.save()

            assertTrue(file.exists())

            val repository2 = FileNodeRepository(file)
            val manager2 = NodeManagerImpl(repository2)
            val root2 = manager2.getRootNode()

            assertEquals(root, root2)
            assertEquals(SINGLE_ROOT, file.readText())
        } finally {
            file.delete()
        }
    }

    private companion object {
        const val SINGLE_ROOT = """{
  "id" : 1000,
  "name" : "Root",
  "type" : "Root"
}"""
    }
}
