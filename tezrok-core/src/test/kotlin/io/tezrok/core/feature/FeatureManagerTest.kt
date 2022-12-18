package io.tezrok.core.feature

import io.tezrok.core.factory.Factory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import java.text.SimpleDateFormat
import java.util.*


@Disabled
class FeatureManagerTest {
    @Test
    fun loadFeaturesTest() {
        val factory = mock(Factory::class.java)
        val features = FeatureManager(factory).features()

        assertEquals(3, features.size)

        features.forEach { println(it) }
    }

    @Test
    fun testFormat() {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        println(sdf.format(Date()))
    }
}