package io.tezrok.core

import io.tezrok.factory.MainFactory
import io.tezrok.generator.MainAppGenerator
import org.junit.jupiter.api.Test

class MainAppGeneratorTest {
    @Test
    fun testMainAppGenerator() {
        val mainFactory = MainFactory()
        val generator = mainFactory.create(MainAppGenerator::class.java)

        generator.generate()
    }
}
