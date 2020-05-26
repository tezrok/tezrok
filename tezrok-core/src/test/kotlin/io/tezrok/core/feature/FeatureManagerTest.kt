package io.tezrok.core.feature

import io.tezrok.core.factory.Factory
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals

class FeatureManagerTest {
    @Test
    fun loadFeaturesTest() {
        val factory = mock(Factory::class.java)
        val features = FeatureManager(factory).features()

        assertEquals(3, features.size)

        features.forEach { println(it) }
    }
}