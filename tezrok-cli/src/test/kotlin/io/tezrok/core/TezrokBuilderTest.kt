package io.tezrok.core

import io.tezrok.util.PathUtil
import io.tezrok.util.ResourceUtil
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Test for [TezrokBuilder]
 */
@Disabled("Manual test")
internal class TezrokBuilderTest {
    @Test
    fun testGenerateProject() {
        val projectPath = ResourceUtil.getResourceAsPath("/projects/tezrok-simple.json")
        val projectOutput = PathUtil.resolve("../../output/tezrok-simple")

        TezrokBuilder.from(projectPath)
            .setOutput(projectOutput)
            .generate()
    }
}
