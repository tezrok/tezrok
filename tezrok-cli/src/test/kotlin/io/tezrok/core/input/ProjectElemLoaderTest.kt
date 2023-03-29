package io.tezrok.core.input

import io.tezrok.BaseTest
import io.tezrok.util.ResourceUtil
import io.tezrok.util.resourceAsString
import io.tezrok.util.toPrettyJson
import org.junit.jupiter.api.Test

internal class ProjectElemLoaderTest : BaseTest() {
    private val loader = ProjectElemLoader()

    @Test
    fun testLoad() {
        val projectPath = ResourceUtil.getResourceAsPath("/projects/tezrok-simple.json")
        val project = loader.load(projectPath)

        val actual = project.toPrettyJson()
        val expected = "/projects/tezrok-simple.expected.json".resourceAsString()

        assertJsonEquals(expected, actual)
    }
}