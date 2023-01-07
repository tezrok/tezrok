package com.tezrok.core

import com.tezrok.api.tree.Node
import com.tezrok.api.tree.NodeManager
import com.tezrok.core.tree.NodeManagerImpl
import com.tezrok.core.tree.repo.file.FileNodeRepository
import com.tezrok.core.feature.FeatureManager
import com.tezrok.core.plugin.PluginManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.util.*

internal abstract class BaseTest {
    protected val pluginManager: PluginManager = PluginManager()
    protected val featureManager: FeatureManager = FeatureManager(pluginManager)
    private val tempDir: File = File(System.getProperty("java.io.tmpdir"))
    protected lateinit var file: File

    @BeforeEach
    open fun setUp() {
        file = File(tempDir, UUID.randomUUID().toString())
    }

    @AfterEach
    open fun tearDown() {
        if (file.exists()) {
            file.delete()
        }
    }

    protected fun nodeManagerFromFile(file: File): NodeManager {
        return NodeManagerImpl(FileNodeRepository(file), featureManager)
    }

    protected fun <T> assertEmpty(collection: Collection<T>) {
        assertTrue(collection.isEmpty()) { "Collection must be empty, but found: $collection" }
    }

    protected fun assertEquals(expected: Node, actual: Node) {
        Assertions.assertEquals(expected, actual)
        Assertions.assertEquals(expected.getChildrenSize(), actual.getChildrenSize())

        val actualChildren = actual.getChildren().toList()

        expected.getChildren().toList().forEachIndexed { index, node ->
            assertEquals(node, actualChildren[index])
        }
    }
}
