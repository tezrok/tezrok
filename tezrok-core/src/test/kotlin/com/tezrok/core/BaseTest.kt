package com.tezrok.core

import com.tezrok.api.node.Node
import com.tezrok.api.node.NodeProperties
import com.tezrok.core.feature.FeatureManager
import com.tezrok.core.plugin.PluginManager
import com.tezrok.core.tree.AuthorType
import com.tezrok.core.tree.NodeManagerImpl
import com.tezrok.core.tree.PropertyValueManager
import com.tezrok.core.tree.repo.file.FileNodeRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.time.OffsetDateTime
import java.util.*

internal abstract class BaseTest {
    protected val pluginManager: PluginManager = PluginManager()
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

    protected fun nodeManagerFromFile(file: File): NodeManagerImpl {
        val featureManager = FeatureManager(pluginManager)
        val propertyValueManager = PropertyValueManager(pluginManager)
        return NodeManagerImpl(FileNodeRepository(file), featureManager, propertyValueManager)
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

    protected fun assertRecentTime(time: OffsetDateTime?) {
        assertNotNull(time, "Time must not be null")
        val now = OffsetDateTime.now()
        assertTrue(time!!.isAfter(now.minusSeconds(5)))
        assertTrue(time.isBefore(now.plusSeconds(5)))
    }

    protected fun createProperties(): NodeProperties {
        val manager = nodeManagerFromFile(file)
        val root = manager.getRootNode()
        manager.startOperation(AuthorType.System, "system")
        return root.getProperties()
    }

    protected companion object {
        val STD_PROPERTIES = setOf("id", "name", "type", "_authorType", "_author", "_createdAt", "_updatedAt")
    }
}
