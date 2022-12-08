package com.tezrok

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.util.*

abstract class BaseTest {
    protected val tempDir: File = File(System.getProperty("java.io.tmpdir"))
    protected val file = File(tempDir, UUID.randomUUID().toString())

    @BeforeEach
    fun setUp() {
        if (file.exists())
            file.delete()
    }

    @AfterEach
    fun tearDown() {
        if (file.exists())
            file.delete()
    }

    protected fun <T> assertEmpty(collection: Collection<T>) {
        Assertions.assertTrue(collection.isEmpty()) { "Collection must be empty, but found: $collection" }
    }
}
