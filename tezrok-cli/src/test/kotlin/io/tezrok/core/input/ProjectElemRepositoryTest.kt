package io.tezrok.core.input

import io.tezrok.BaseTest
import io.tezrok.core.feature.FeatureManager
import io.tezrok.util.ResourceUtil
import io.tezrok.util.resourceAsString
import io.tezrok.util.toPrettyJson
import org.junit.jupiter.api.Test

internal class ProjectElemRepositoryTest : BaseTest() {
    private val loader = ProjectElemRepository()
    private val featureManager = FeatureManager()

    @Test
    fun testLoad_FromJsonWithSchema() {
        val projectPath = ResourceUtil.getResourceAsPath("/projects/tezrok-simple.json")
        val project = loader.load(projectPath, featureManager::processModel)

        val actual = project.toPrettyJson()
        val expected = "/projects/tezrok-simple.expected.json".resourceAsString()

        assertJsonEquals(expected, actual)
    }
}
